package com.example.mpos

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zoop.pos.Zoop
import com.zoop.pos.collection.SmsParameters
import com.zoop.pos.collection.TransactionData
import com.zoop.pos.collection.UserSelection
import com.zoop.pos.exception.ZoopRequestCanceledException
import com.zoop.pos.plugin.DashboardConfirmationResponse
import com.zoop.pos.plugin.DashboardThemeResponse
import com.zoop.pos.plugin.DashboardTokenResponse
import com.zoop.pos.plugin.HttpTableLoadResponse
import com.zoop.pos.plugin.ZoopFoundationPlugin
import com.zoop.pos.requestfield.MessageCallbackRequestField
import com.zoop.pos.requestfield.QRCodeCallbackRequestField
import com.zoop.pos.type.Callback
import com.zoop.pos.type.Option
import com.zoop.pos.type.Request
import com.zoop.sdk.plugin.mpos.MPOSPlugin
import com.zoop.sdk.plugin.mpos.bluetooth.platform.BluetoothDevice
import com.zoop.sdk.plugin.mpos.request.PairingStatus
import com.zoop.sdk.plugin.mpos.request.PayResponse
import com.zoop.sdk.plugin.mpos.request.mPOSDiscoveryResponse
import com.zoop.sdk.plugin.mpos.request.mPOSPixPaymentResponse
import com.zoop.sdk.plugin.mpos.request.mPOSSendSMSResponse
import com.zoop.sdk.plugin.mpos.request.mPOSTableLoadResponse
import com.zoop.sdk.plugin.mpos.request.mPOSVoidResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds


class MainViewModel : ViewModel() {

    var state by mutableStateOf(MainState(status = Status.NONE))
        private set
    private var voidTransaction: UserSelection<TransactionData>? = null
    private var paymentRequest: Request? = null
    private var voidRequest: Request? = null
    private var loginRequest: Request? = null
    private var pixRequest: Request? = null
    private lateinit var bluetoothDevice: UserSelection<BluetoothDevice>

    private companion object {
        const val TAG = "ExampleMPos"
    }

    fun handle(event: MainEvent) {
        when (event) {
            MainEvent.OnStartBluetooth -> bluetooth()
            MainEvent.OnStartLogin -> login()
            is MainEvent.OnOptionPayment -> onOptionPayment()
            is MainEvent.OnStartPayment -> payment(
                amount = event.amount,
                installments = event.installment,
                option = event.option
            )

            is MainEvent.OnStartPix -> pix(event.amount)
            MainEvent.OnStartCancellation -> void()
            MainEvent.OnCancelAction -> cancelAction()
            MainEvent.OnDisplayNone -> restoreUI()
            is MainEvent.OnWriteDisplay -> writeToDisplay(event.message)
            is MainEvent.OnSelectTransaction -> {
                voidTransaction?.select(event.transaction)
            }

            is MainEvent.OnSelectBtDevice -> bluetoothDevice.select(event.device)
            MainEvent.TableLoad -> tableLoad()
        }
    }

    private fun restoreUI() {
        state = state.copy(status = Status.NONE)
    }

    private fun onOptionPayment() {
        state = state.copy(status = Status.DISPLAY_OPTION_PAYMENT)
    }

    private fun cancelAction() {
        paymentRequest?.cancel() ?: pixRequest?.cancel() ?: voidTransaction?.cancel()
        ?: voidRequest?.cancel() ?: loginRequest?.cancel()
    }

    private fun bluetooth() {
        MPOSPlugin.createDiscoveryRequestBuilder()
            .time(6L, TimeUnit.SECONDS)
            .callback(object : Callback<mPOSDiscoveryResponse>() {
                override fun onStart() {
                    state = state.copy(
                        status = Status.MESSAGE,
                        message = "Buscando dispositivos"
                    )
                }

                override fun onFail(error: Throwable) {
                    Log.d(TAG, "Falha ao buscar dispositivos ${error.stackTraceToString()}")
                    state =
                        state.copy(
                            status = Status.MESSAGE,
                            message = "Falha ao buscar dispositivos"
                        )
                }

                override fun onSuccess(response: mPOSDiscoveryResponse) {
                    Log.d(TAG, "Dispositivos encontrados: ${response.available.items}")
                    bluetoothDevice = response.available
                    state = state.copy(
                        status = Status.DISPLAY_BLUETOOTH_LIST,
                        bluetoothDevices = response.available.items.toList()
                    )
                }

            })
            .pairingCallback(object : Callback<PairingStatus>() {
                override fun onFail(error: Throwable) {
                    Log.d(TAG, "Falha ao parear dispositivo")
                    state =
                        state.copy(status = Status.MESSAGE, message = "Falha ao parear dispositivo")
                }

                override fun onSuccess(response: PairingStatus) {
                    val message = if (response.status) {
                        "Dispositivo pareado com sucesso"
                    } else {
                        "Falha ao parear dispositivo"
                    }
                    state =
                        state.copy(status = Status.MESSAGE, message = message)
                }

            }).build().run(Zoop::post)
    }

    private fun login() {
        paymentRequest = null
        voidRequest = null
        loginRequest = ZoopFoundationPlugin.createDashboardActivationRequestBuilder()
            .tokenCallback(object : Callback<DashboardTokenResponse>() {
                override fun onStart() {
                    state = state.copy(status = Status.MESSAGE, message = "Requisitando token")
                }

                override fun onFail(error: Throwable) {
                    Log.d(TAG, "Falha ao requisitar token")
                    state =
                        state.copy(status = Status.MESSAGE, message = "Falha ao requisitar token")
                }

                override fun onSuccess(response: DashboardTokenResponse) {
                    Log.d(TAG, "Apresentar token ao usuário: ${response.token}")
                    state = state.copy(
                        status = Status.MESSAGE,
                        message = "Insira o token no dashboard: ${response.token}"
                    )
                }

            })
            .confirmCallback(object : Callback<DashboardConfirmationResponse>() {
                override fun onFail(error: Throwable) {
                    /**
                    Caso o login seja cancelado, receberá a resposta aqui, com mensagem "request canceled"
                    loginRequest.cancel()
                     */
                    Log.d(TAG, "Apresentar erro na confirmação do token: ${error.message}")
                    state = when (error) {
                        is ZoopRequestCanceledException -> state.copy(
                            status = Status.MESSAGE,
                            message = "Operação cancelada"
                        )

                        else -> state.copy(
                            status = Status.MESSAGE,
                            message = error.message.toString()
                        )
                    }
                }

                override fun onSuccess(response: DashboardConfirmationResponse) {
                    /**
                     * Nesse ponto, recomendamos guardar as credenciais localmente em um banco de dados/shared preferences,
                     * para usar na próxima inicialização, passando como parâmetro para o MPOSPluginManager
                     */
                    Log.d(TAG, "Aqui, você recebe as credenciais do estabelecimento")
                    Log.d(TAG, "MarketplaceId: ${response.credentials.marketplace}")
                    Log.d(TAG, "SellerId: ${response.credentials.seller}")
                    Log.d(TAG, "Terminal: ${response.credentials.terminal}")
                    Log.d(TAG, "AccessKey: ${response.credentials.accessKey}")
                    Log.d(TAG, "SellerName: ${response.owner.name}")
                    state = state.copy(
                        status = Status.MESSAGE,
                        message = "SellerName: ${response.owner.name}"
                    )
                }

            })
            .themeCallback(object : Callback<DashboardThemeResponse>() {
                override fun onStart() {
                    if (loginRequest?.isCancelRequested == true) return
                    state = state.copy(status = Status.MESSAGE, message = "Baixando temas")
                }

                override fun onFail(error: Throwable) {
                    Log.d(TAG, "Apresentar erro no download de temas: ${error.message}")
                    state = state.copy(status = Status.MESSAGE, message = error.message.toString())
                }

                override fun onSuccess(response: DashboardThemeResponse) {
                    /**
                     * Aqui você recebe o esquema de cores configurado para o seller no dashboard,
                     * e também sinaliza o sucesso no fluxo de ativação do terminal.
                     */
                    Log.d(TAG, "Exemplo de cor de fonte ${response.color.font}")
                    Log.d(TAG, "Exemplo de cor de botão ${response.color.button}")
                    Log.d(TAG, "Exemplo de logo colorido ${response.logo.coloredBase64}")
                    state = state.copy(status = Status.MESSAGE, message = "Login realizado")

                }

            }).build()

        Zoop.post(loginRequest!!)

    }

    private fun payment(amount: Long, installments: Int, option: Option) {
        loginRequest = null
        voidRequest = null
        Log.d(
            TAG,
            "payment: ${
                String.format(
                    "amount: %s, installments: %s, option: %s",
                    amount,
                    installments,
                    option
                )
            }"
        )

        paymentRequest = MPOSPlugin.createPaymentRequestBuilder()
            .amount(amount)
            .option(option)
            .installments(installments)
            .callback(object : Callback<PayResponse>() {
                override fun onStart() {
                    Log.d(TAG, "onStart")
                    state = state.copy(status = Status.MESSAGE, message = "Iniciando")
                }

                override fun onSuccess(response: PayResponse) {
                    Log.d(TAG, "onSuccess")
                    state = state.copy(status = Status.MESSAGE, message = "SUCESSO")
                }

                override fun onFail(error: Throwable) {
                    error.printStackTrace()
                    Log.e(TAG, "onFail ${error.message}")
                    val message = if (error.message?.contains("invalid session") == true) {
                        "Não foi realizado um login"
                    } else {
                        error.message
                    }

                    state = state.copy(status = Status.MESSAGE, message = message ?: "Falha")
                }

            })
            .messageCallback(object : Callback<MessageCallbackRequestField.MessageData>() {
                override fun onSuccess(response: MessageCallbackRequestField.MessageData) {
                    Log.d(TAG, "messageCallback ${response.message}")
                    state = state.copy(status = Status.MESSAGE, message = response.message)
                }

                override fun onFail(error: Throwable) {
                    error.printStackTrace()
                    Log.e(TAG, "messageCallback fail")
                }
            })
            .build()

        Zoop.post(paymentRequest!!)
    }

    private fun pix(amount: Long) {
        Log.d(TAG, "pix: ${String.format("amount: %s", amount)}")

        pixRequest = MPOSPlugin.createPixPaymentRequestBuilder()
            .amount(amount)
            .callback(object : Callback<mPOSPixPaymentResponse>() {
                override fun onStart() {
                    state = state.copy(status = Status.MESSAGE, message = "Iniciando")
                }

                override fun onSuccess(response: mPOSPixPaymentResponse) {
                    state = state.copy(status = Status.MESSAGE, message = "SUCESSO")
                }

                override fun onFail(error: Throwable) {
                    val message = if (error.message?.contains("invalid session") == true) {
                        "Não foi realizado um login"
                    } else {
                        error.message
                    }

                    state = state.copy(status = Status.MESSAGE, message = message ?: "Falha")
                }

            })
            .messageCallback(object : Callback<MessageCallbackRequestField.MessageData>() {
                override fun onSuccess(response: MessageCallbackRequestField.MessageData) {
                    state = state.copy(status = Status.MESSAGE, message = response.message)
                }

                override fun onFail(error: Throwable) {
                }
            })
            .qrCodeCallback(object : Callback<QRCodeCallbackRequestField.QRCodeData>() {
                override fun onSuccess(response: QRCodeCallbackRequestField.QRCodeData) {
                    Log.d(TAG, "onSuccess: response ${response.data}")
                    state = state.copy(status = Status.QR_CODE, qrCode = response.data)
                }

                override fun onFail(error: Throwable) {

                }
            }).build()

        Zoop.post(pixRequest!!)
    }

    private fun sms(transactionData: TransactionData) {
        val phoneNumber = "5511999999999"
        MPOSPlugin.createSendSMSRequestBuilder()
            .smsParameters(
                SmsParameters(
                    transactionData,
                    phoneNumber = phoneNumber
                )
            )
            .callback(object : Callback<mPOSSendSMSResponse>() {
                override fun onFail(error: Throwable) {
                    Log.d(TAG, "onFail SMS ${error.message}")
                }

                override fun onSuccess(response: mPOSSendSMSResponse) {
                    Log.d(TAG, "onSuccess send SMS: $response")
                }
            }).build().run(Zoop::post)
    }

    private fun tableLoad() {
        val tableLoadRequest = MPOSPlugin.createTableLoadRequestBuilder()
            .callback(object : Callback<HttpTableLoadResponse>() {
                override fun onStart() {
                    state = state.copy(status = Status.MESSAGE, message = "Iniciando")
                }

                override fun onSuccess(response: mPOSTableLoadResponse) {
                    state = state.copy(status = Status.MESSAGE, message = "SUCESSO")
                }

                override fun onFail(error: Throwable) {
                    error.printStackTrace()
                    Log.d(TAG, "onFail: ${error.localizedMessage}")

                    val message = if (error.message?.contains("invalid session") == true) {
                        "Não foi realizado um login"
                    } else {
                        error.message
                    }
                    state = state.copy(status = Status.MESSAGE, message = message ?: "Falha")
                }

            })
            .messageCallback(object : Callback<MessageCallbackRequestField.MessageData>() {
                override fun onSuccess(response: MessageCallbackRequestField.MessageData) {
                    state = state.copy(status = Status.MESSAGE, message = response.message)
                }

                override fun onFail(error: Throwable) {
                    error.printStackTrace()
                    Log.d(TAG, "onFail: ${error.localizedMessage}")
                }
            })
            .build()

        Zoop.post(tableLoadRequest)
    }

    private fun void() {
        loginRequest = null
        paymentRequest = null
        voidRequest = MPOSPlugin.createVoidRequestBuilder()
            .callback(object : Callback<mPOSVoidResponse>() {
                override fun onStart() {
                    state = state.copy(status = Status.MESSAGE, message = "Processando")
                }

                override fun onSuccess(response: mPOSVoidResponse) {
                    Log.d(TAG, "onSuccess: response $response")
                    state = state.copy(status = Status.MESSAGE, message = "Cancelamento realizado")
                    voidTransaction = null
                }

                override fun onFail(error: Throwable) {
                    error.printStackTrace()
                    Log.d(TAG, "callback onFail: error ${error.message}")

                    state = state.copy(
                        status = Status.MESSAGE,
                        message = error.message ?: "Falha na operação"
                    )
                    voidTransaction = null
                }

                override fun onComplete() {
                    viewModelScope.launch(Dispatchers.Main) {
                        delay(5.seconds)
                        state = state.copy(status = Status.FINISHED, message = "")
                    }
                }
            })
            .voidTransactionCallback(object : Callback<UserSelection<TransactionData>>() {
                override fun onSuccess(response: UserSelection<TransactionData>) {
                    voidTransaction = response
                    state = state.copy(
                        transactionsList = voidTransaction!!.items.toList(),
                        status = Status.DISPLAY_VOID_LIST
                    )
                }

                override fun onFail(error: Throwable) {
                    error.printStackTrace()
                    Log.d(TAG, "voidTransactionCallback onFail: error ${error.localizedMessage}")
                    state = state.copy(
                        status = Status.MESSAGE,
                        message = error.message ?: "Falha na operação"
                    )
                }
            })
            .messageCallback(object : Callback<MessageCallbackRequestField.MessageData>() {
                override fun onSuccess(response: MessageCallbackRequestField.MessageData) {
                    state = state.copy(status = Status.MESSAGE, message = response.message)
                }

                override fun onFail(error: Throwable) {
                    error.printStackTrace()
                    Log.d(TAG, "messageCallback onFail: error ${error.localizedMessage}")
                    state = state.copy(
                        status = Status.MESSAGE,
                        message = error.message ?: "Falha na operação"
                    )
                }
            })
            .build()

        Zoop.post(voidRequest!!)
    }

    private fun writeToDisplay(message: String) {
        MPOSPlugin.createWriteToDisplayRequestBuilder()
            .messageDisplay(message)
            .clearDisplay(true)
            .callback(object : Callback<Boolean>() {
                override fun onFail(error: Throwable) {
                    Log.d(TAG, "fail on posting to pinPad display ${error.message}")
                }

                override fun onSuccess(response: Boolean) {
                    Log.d(TAG, "success on posting to pinPad display")
                }

            })
            .build()
            .run(Zoop::post)
    }
}