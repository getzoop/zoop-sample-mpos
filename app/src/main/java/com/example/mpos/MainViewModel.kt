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
import com.zoop.pos.collection.VoidTransaction
import com.zoop.pos.exception.ZoopRequestCanceledException
import com.zoop.pos.plugin.DashboardConfirmationResponse
import com.zoop.pos.plugin.DashboardThemeResponse
import com.zoop.pos.plugin.DashboardTokenResponse
import com.zoop.pos.plugin.HttpTableLoadResponse
import com.zoop.pos.plugin.ZoopFoundationPlugin
import com.zoop.pos.requestfield.MessageCallbackRequestField
import com.zoop.pos.requestfield.QRCodeCallbackRequestField
import com.zoop.pos.requestfield.TransactionIdCallbackRequestField
import com.zoop.pos.type.Callback
import com.zoop.pos.type.Option
import com.zoop.pos.type.Request
import com.zoop.sdk.plugin.mpos.MPOSPlugin
import com.zoop.sdk.plugin.mpos.bluetooth.platform.BluetoothDevice
import com.zoop.sdk.plugin.mpos.request.PairingStatus
import com.zoop.sdk.plugin.mpos.request.mPOSDiscoveryResponse
import com.zoop.sdk.plugin.mpos.request.mPOSPaymentResponse
import com.zoop.sdk.plugin.mpos.request.mPOSPixPaymentResponse
import com.zoop.sdk.plugin.mpos.request.mPOSSendSMSResponse
import com.zoop.sdk.plugin.mpos.request.mPOSTableLoadResponse
import com.zoop.sdk.plugin.mpos.request.mPOSVoidResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

class MainViewModel : ViewModel() {
    var state by mutableStateOf(MainState(status = Status.NONE))
        private set
    private var currentCancellableRequest: Request? = null
    private var voidTransaction: UserSelection<VoidTransaction>? = null
    private var latestTransaction: TransactionData? = null
    private lateinit var bluetoothDevice: UserSelection<BluetoothDevice>

    private companion object {
        const val TAG = "ExampleMPos"
    }

    fun handle(event: MainEvent) {
        when (event) {
            is MainEvent.OnStartBluetooth -> bluetooth()
            is MainEvent.OnStartLogin -> login()
            is MainEvent.OnOptionPayment -> onOptionPayment()
            is MainEvent.OnStartPayment -> payment(
                amount = event.amount,
                installments = event.installment,
                option = event.option
            )

            is MainEvent.OnStartPix -> pix(event.amount)
            is MainEvent.OnStartCancellation -> void()
            is MainEvent.OnCancelAction -> cancelAction()
            is MainEvent.OnDisplayNone -> restoreUI()
            is MainEvent.OnWriteDisplay -> writeToDisplay(event.message)
            is MainEvent.OnSelectTransaction -> selectTransactionToVoid(event)
            is MainEvent.OnSelectBtDevice -> bluetoothDevice.select(event.device)
            is MainEvent.OnTableLoad -> tableLoad()
            is MainEvent.OnStartSms -> askPhoneNumber()
            is MainEvent.OnSendSms -> sendSms(event.phoneNumber)
        }
    }

    private fun restoreUI() {
        state = MainState(status = Status.NONE)
    }

    private fun onOptionPayment() {
        state = state.copy(status = Status.DISPLAY_OPTION_PAYMENT)
    }

    private fun cancelAction() {
        if (state.status == Status.DISPLAY_BLUETOOTH_LIST) {
            state = state.copy(
                status = Status.NONE
            )
        }

        voidTransaction?.also {
            it.cancel()
        } ?: currentCancellableRequest?.also {
            currentCancellableRequest = null
            it.cancel()
        }
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
                    error.printStackTrace()
                    Log.e(TAG, "Falha ao buscar dispositivos ${error.stackTraceToString()}")
                    state =
                        state.copy(
                            status = Status.MESSAGE,
                            message = "Falha ao buscar dispositivos"
                        )

                    updateStatusToFinish()
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
                        state.copy(
                            status = Status.MESSAGE,
                            message = "Falha ao parear dispositivo"
                        )

                    updateStatusToFinish()
                }

                override fun onSuccess(response: PairingStatus) {
                    val message = if (response.status) {
                        "Dispositivo pareado com sucesso"
                    } else {
                        "Falha ao parear dispositivo"
                    }
                    state = state.copy(status = Status.MESSAGE, message = message)

                    updateStatusToFinish()
                }
            })
            .build()
            .run(Zoop::post)
    }

    private fun login() {
        voidTransaction = null

        val loginRequest = ZoopFoundationPlugin.createDashboardActivationRequestBuilder()
            .tokenCallback(object : Callback<DashboardTokenResponse>() {
                override fun onStart() {
                    state = state.copy(status = Status.MESSAGE, message = "Requisitando token")
                }

                override fun onFail(error: Throwable) {
                    Log.d(TAG, "Falha ao requisitar token")
                    state =
                        state.copy(status = Status.MESSAGE, message = "Falha ao requisitar token")

                    updateStatusToFinish()
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
                        is ZoopRequestCanceledException -> {
                            state.copy(
                                status = Status.MESSAGE,
                                message = "Operação cancelada"
                            )
                        }

                        else -> state.copy(
                            status = Status.MESSAGE,
                            message = error.message.toString()
                        )
                    }

                    updateStatusToFinish()
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
                    state = state.copy(status = Status.MESSAGE, message = "Baixando temas")
                }

                override fun onFail(error: Throwable) {
                    Log.d(TAG, "Apresentar erro no download de temas: ${error.message}")
                    state = state.copy(status = Status.MESSAGE, message = error.message.toString())

                    updateStatusToFinish()
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

                    updateStatusToFinish()
                }
            })
            .build()

        Zoop.post(loginRequest)
    }

    private fun payment(amount: Long, installments: Int, option: Option) {
        voidTransaction = null

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

        val paymentRequest = MPOSPlugin.createPaymentRequestBuilder()
            .amount(amount)
            .option(option)
            .installments(installments)
            .callback(object : Callback<mPOSPaymentResponse>() {
                override fun onStart() {
                    Log.d(TAG, "onStart")
                    state = state.copy(status = Status.MESSAGE, message = "Iniciando")
                }

                override fun onSuccess(response: mPOSPaymentResponse) {
                    Log.d(TAG, "onSuccess")

                    latestTransaction = response.transactionData
                    state = state.copy(
                        status = Status.MESSAGE,
                        message = "Pagamento aprovado com sucesso"
                    )
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

                override fun onComplete() {
                    updateStatusToFinish()
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
                    state = state.copy(
                        status = Status.MESSAGE,
                        message = error.message ?: "Falha no pagamento"
                    )
                    updateStatusToFinish()
                }
            })
            .build()
            .also { currentCancellableRequest = it }

        Zoop.post(paymentRequest)
    }

    private fun pix(amount: Long) {
        Log.d(TAG, "pix: ${String.format("amount: %s", amount)}")

        voidTransaction = null

        val pixRequest = MPOSPlugin.createPixPaymentRequestBuilder()
            .amount(amount)
            .callback(object : Callback<mPOSPixPaymentResponse>() {
                override fun onStart() {
                    state = state.copy(status = Status.MESSAGE, message = "Iniciando")
                }

                override fun onSuccess(response: mPOSPixPaymentResponse) {
                    latestTransaction = response.transactionData
                    state = state.copy(status = Status.MESSAGE, message = "Pix aprovado com sucesso")
                }

                override fun onFail(error: Throwable) {
                    val message = if (error.message?.contains("invalid session") == true) {
                        "Não foi realizado um login"
                    } else {
                        error.message
                    }
                    state = state.copy(status = Status.MESSAGE, message = message ?: "Falha")
                }

                override fun onComplete() {
                    updateStatusToFinish()
                }
            })
            .messageCallback(object : Callback<MessageCallbackRequestField.MessageData>() {
                override fun onSuccess(response: MessageCallbackRequestField.MessageData) {
                    state = state.copy(status = Status.MESSAGE, message = response.message)
                }

                override fun onFail(error: Throwable) {
                    error.printStackTrace()
                    state = state.copy(
                        status = Status.MESSAGE,
                        message = error.message ?: "Falha no pagamento"
                    )
                    updateStatusToFinish()
                }
            })
            .qrCodeCallback(object : Callback<QRCodeCallbackRequestField.QRCodeData>() {
                override fun onSuccess(response: QRCodeCallbackRequestField.QRCodeData) {
                    Log.d(TAG, "onSuccess: response ${response.data}")
                    state = state.copy(status = Status.QR_CODE, qrCode = response.data)
                }

                override fun onFail(error: Throwable) {
                    error.printStackTrace()
                    state = state.copy(
                        status = Status.MESSAGE,
                        message = error.message ?: "Falha no QRcode"
                    )
                    updateStatusToFinish()
                }
            })
            .transactionIdCallback(object : Callback<TransactionIdCallbackRequestField.transactionIdData>() {
                override fun onFail(error: Throwable) {
                    Log.e(TAG, "onFail: error ${error.localizedMessage}")

                }

                override fun onSuccess(response: TransactionIdCallbackRequestField.transactionIdData) {
                    Log.d(TAG, "onSuccess: response ${response.data}")
                }
            })
            .build()
            .also { currentCancellableRequest = it }

        Zoop.post(pixRequest)
    }

    private fun askPhoneNumber() {
        if (latestTransaction == null) {
            state = state.copy(
                status = Status.MESSAGE,
                message = "Nenhuma transação recente para enviar SMS"
            )

            updateStatusToFinish()

            return
        }

        state = MainState(status = Status.ASK_PHONE_NUMBER)
    }

    private fun sendSms(phoneNumber: String) {
        val transactionData = latestTransaction ?: run {
            state = state.copy(
                status = Status.MESSAGE,
                message = "Nenhuma transação recente para enviar SMS"
            )

            updateStatusToFinish()

            return
        }

        val sanitizedPhoneNumber =
            if (phoneNumber.length >= 12 && phoneNumber.startsWith("55")) {
                phoneNumber
            } else {
                "55$phoneNumber"
            }

        val smsRequest = MPOSPlugin.createSendSMSRequestBuilder()
            .smsParameters(
                SmsParameters(
                    transactionData = transactionData,
                    phoneNumber = sanitizedPhoneNumber,
                )
            )
            .callback(object : Callback<mPOSSendSMSResponse>() {
                override fun onStart() {
                    state = MainState(status = Status.SENDING_SMS)
                }

                override fun onFail(error: Throwable) {
                    Log.d(TAG, "onFail SMS ${error.message}")
                    state = state.copy(
                        status = Status.MESSAGE,
                        message = error.message ?: "Falha ao enviar SMS",
                    )
                }

                override fun onSuccess(response: mPOSSendSMSResponse) {
                    Log.d(TAG, "onSuccess send SMS: $response")
                    state = state.copy(
                        status = Status.MESSAGE,
                        message = "SMS enviado com sucesso!",
                    )
                }

                override fun onComplete() {
                    updateStatusToFinish()
                }
            })
            .build()

        Zoop.post(smsRequest)
    }

    private fun tableLoad() {
        val tableLoadRequest = MPOSPlugin.createTableLoadRequestBuilder()
            .callback(object : Callback<HttpTableLoadResponse>() {
                override fun onStart() {
                    state = state.copy(status = Status.MESSAGE, message = "Iniciando")
                }

                override fun onSuccess(response: mPOSTableLoadResponse) {
                    Log.d(TAG, "onSuccess: response $response")
                    state = state.copy(
                        status = Status.MESSAGE,
                        message = "Tabela atualizada com sucesso"
                    )
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

                override fun onComplete() {
                    updateStatusToFinish()
                }
            })
            .messageCallback(object : Callback<MessageCallbackRequestField.MessageData>() {
                override fun onSuccess(response: MessageCallbackRequestField.MessageData) {
                    state = state.copy(status = Status.MESSAGE, message = response.message)
                }

                override fun onFail(error: Throwable) {
                    error.printStackTrace()
                    Log.d(TAG, "onFail: ${error.localizedMessage}")
                    state = state.copy(
                        status = Status.MESSAGE,
                        message = error.message ?: "Falha na carga de tabela"
                    )

                    updateStatusToFinish()
                }
            })
            .build()

        Zoop.post(tableLoadRequest)
    }

    private fun void() {
        voidTransaction = null

        val voidRequest = MPOSPlugin.createVoidRequestBuilder()
            .callback(object : Callback<mPOSVoidResponse>() {
                override fun onStart() {
                    state = state.copy(status = Status.MESSAGE, message = "Processando")
                }

                override fun onSuccess(response: mPOSVoidResponse) {
                    Log.d(TAG, "onSuccess: response $response")
                    latestTransaction = response.transactionData
                    state = state.copy(
                        status = Status.MESSAGE,
                        message = "Cancelamento realizado"
                    )
                }

                override fun onFail(error: Throwable) {
                    error.printStackTrace()
                    Log.d(TAG, "callback onFail: error ${error.message}")

                    state = state.copy(
                        status = Status.MESSAGE,
                        message = error.message ?: "Falha na operação"
                    )
                }

                override fun onComplete() {
                    voidTransaction = null

                    updateStatusToFinish()
                }
            })
            .voidTransactionCallback(object : Callback<UserSelection<VoidTransaction>>() {
                override fun onSuccess(response: UserSelection<VoidTransaction>) {
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

                    updateStatusToFinish()
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

                    updateStatusToFinish()
                }
            })
            .build()
            .also { currentCancellableRequest = it }

        Zoop.post(voidRequest)
    }

    private fun selectTransactionToVoid(event: MainEvent.OnSelectTransaction) {
        voidTransaction?.select(event.transaction)

        state = state.copy(
            status = Status.MESSAGE,
            message = "Processando…",
        )
    }

    private fun updateStatusToFinish() {
        viewModelScope.launch {
            delay(3.seconds)
            state = state.copy(status = Status.FINISHED, message = "")
        }
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