package com.example.mpos

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mpos.extension.convertToLong
import com.example.mpos.ui.theme.SmartPOSTheme
import com.example.mpos.util.LifecycleOwnerPermission
import com.example.mpos.util.MPOSPluginManager
import com.example.mpos.util.rememberQrBitmapPainter
import com.zoop.pos.Zoop
import com.zoop.pos.type.Option
import com.zoop.sdk.plugin.mpos.MPOSPlugin

class MainActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel = MainViewModel()
        setContent {
            SmartPOSTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    titleContentColor = MaterialTheme.colorScheme.primary,
                                ),
                                title = {
                                    Text(
                                        modifier = Modifier.fillMaxWidth(),
                                        text = stringResource(id = R.string.app_name),
                                        textAlign = TextAlign.Center,
                                        color = Color.White
                                    )
                                }
                            )
                        },
                    ) {
                        MainScreen(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    LifecycleOwnerPermission()
    MPOSPluginManager().initialize(LocalContext.current)
    var isPaymentPix by remember { mutableStateOf(false) }

    MPOSPlugin(Zoop.constructorParameters()).run(Zoop::plug)
    Log.d("MainScreen", "Status: ${viewModel.state.status}")

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        Row(modifier = Modifier.padding(15.dp)) {
            Button(
                onClick = { viewModel.handle(MainEvent.OnStartLogin) },
                modifier = Modifier.padding(5.dp)
            ) {
                Text(
                    text = "Login",
                    modifier = Modifier.padding(8.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Button(
                onClick = { viewModel.handle(MainEvent.OnStartBluetooth) },
                modifier = Modifier.padding(5.dp)
            ) {
                Text(
                    text = "Bluetooth",
                    modifier = Modifier.padding(8.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Row(modifier = Modifier.padding(15.dp)) {
            Button(
                onClick = {
                    viewModel.handle(MainEvent.OnOptionPayment)
                    isPaymentPix = false
                },
                modifier = Modifier.padding(5.dp)
            ) {
                Text(
                    text = "Venda",
                    modifier = Modifier.padding(8.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Button(
                onClick = {
                    viewModel.handle(MainEvent.OnOptionPayment)
                    isPaymentPix = true
                },
                modifier = Modifier.padding(5.dp)
            ) {
                Text(
                    text = "Pix",
                    modifier = Modifier.padding(8.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Row(modifier = Modifier.padding(15.dp)) {
            Button(
                onClick = { viewModel.handle(MainEvent.OnStartCancellation) },
                modifier = Modifier.padding(5.dp)
            ) {
                Text(
                    text = "Cancelamento",
                    modifier = Modifier.padding(8.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Button(
                onClick = { viewModel.handle(MainEvent.OnWriteDisplay("Mensagem de teste")) },
                modifier = Modifier.padding(5.dp)
            ) {
                Text(
                    text = "Mensagem na Tela",
                    modifier = Modifier.padding(8.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        AnimatedVisibility(visible = viewModel.state.status == Status.MESSAGE) {
            Text(text = viewModel.state.message)
        }

        AnimatedVisibility(
            visible = viewModel.state.message.contains(
                stringResource(R.string.enter),
                true
            )
        ) {
            CancelButton(handler = viewModel::handle)
        }

        AnimatedVisibility(visible = viewModel.state.status == Status.QR_CODE) {
            Column(
                modifier = Modifier.padding(top = 15.dp, bottom = 15.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = rememberQrBitmapPainter(viewModel.state.qrCode),
                    contentDescription = stringResource(R.string.qRCode_token),
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.size(135.dp),
                    alignment = Alignment.Center
                )



                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )
                CancelButton(handler = viewModel::handle)
            }
        }

        AnimatedVisibility(visible = viewModel.state.status == Status.DISPLAY_VOID_LIST) {
            Column(
                modifier = Modifier.padding(top = 15.dp, bottom = 15.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AssembleVoidTransactionList(
                    state = viewModel.state,
                    handler = viewModel::handle
                )
                CancelButton(handler = viewModel::handle)
            }
        }

        AnimatedVisibility(visible = viewModel.state.status == Status.DISPLAY_BLUETOOTH_LIST) {
            Column(
                modifier = Modifier.padding(top = 15.dp, bottom = 15.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AssembleBluetoothDevicesList(
                    state = viewModel.state,
                    handler = viewModel::handle
                )
                CancelButton(handler = viewModel::handle)
            }
        }

        AnimatedVisibility(visible = viewModel.state.status == Status.DISPLAY_OPTION_PAYMENT) {
            ChargePaymentOption(
                handleCancel = { viewModel.handle(MainEvent.OnDisplayNone) },
                handleEvent = {
                    val (amount, installment, option) = it
                    if (isPaymentPix) {
                        viewModel.handle(MainEvent.OnStartPix(amount))
                    } else {
                        viewModel.handle(MainEvent.OnStartPayment(amount, installment, option))
                    }
                },
                isPaymentPix = isPaymentPix
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ChargePaymentOption(
    handleEvent: (Triple<Long, Int, Option>) -> Unit,
    handleCancel: () -> Unit,
    isPaymentPix: Boolean = false
) {
    val textFieldValueAmount = remember { mutableStateOf(TextFieldValue(text = "1")) }
    val textFieldValueInstallment = remember { mutableStateOf(TextFieldValue(text = "1")) }
    val radioOptions = listOf(Option.DEBIT, Option.CREDIT)
    val (selectedOption, onOptionSelected) = remember { mutableStateOf(radioOptions[1]) }
    val context = LocalContext.current
    val messageValidAmount = stringResource(id = R.string.message_invalid_amount)
    val messageValidInstallment = stringResource(id = R.string.message_invalid_installment)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        )

        val textTitleOptPayment = if (!isPaymentPix) stringResource(R.string.payment_option)
        else stringResource(id = R.string.enter_the_value).plus(" do pix")

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            textAlign = TextAlign.Center,
            text = textTitleOptPayment.uppercase(),
            fontSize = 18.sp
        )

        if (!isPaymentPix) {
            radioOptions.forEach { text ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (text == selectedOption),
                            onClick = {
                                onOptionSelected(text)
                            }
                        ),
                ) {
                    RadioButton(
                        selected = (text == selectedOption),
                        onClick = { onOptionSelected(text) }
                    )
                    Text(
                        text = text.name,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 4.dp),
                value = textFieldValueAmount.value,
                label = {
                    Text(text = stringResource(R.string.enter_the_value))
                },
                placeholder = {
                    Text(text = stringResource(R.string.enter_the_value))
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = if (selectedOption == Option.DEBIT) ImeAction.Done
                    else ImeAction.Next
                ),
                onValueChange = {
                    textFieldValueAmount.value = it
                }
            )

            if (!isPaymentPix) {
                AnimatedVisibility(
                    modifier = Modifier
                        .weight(.4f),
                    visible = selectedOption == Option.CREDIT
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .padding(start = 4.dp, end = 12.dp),
                        value = textFieldValueInstallment.value,
                        label = {
                            Text(text = stringResource(R.string.installments))
                        },
                        placeholder = {
                            Text(text = stringResource(R.string.installments))
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        onValueChange = {
                            textFieldValueInstallment.value = it
                        }
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        )

        val (amount, installment, option) = Triple(
            textFieldValueAmount.value.text.convertToLong(),
            textFieldValueInstallment.value.text.convertToLong().toInt(),
            selectedOption
        )

        Row(
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                modifier = Modifier
                    .weight(0.5f)
                    .padding(start = 12.dp, end = 4.dp)
                    .border(
                        border = BorderStroke(1.dp, color = Color.LightGray),
                        shape = RoundedCornerShape(24.dp)
                    ),
                onClick = handleCancel
            ) {
                Text(text = stringResource(R.string.button_close))
            }

            Button(
                modifier = Modifier
                    .weight(.5f)
                    .padding(start = 4.dp, end = 12.dp),
                onClick = {
                    if (amount <= 0) {
                        Toast.makeText(context, messageValidAmount, Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (selectedOption == Option.CREDIT && installment <= 0) {
                        Toast.makeText(context, messageValidInstallment, Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    handleEvent(Triple(amount, installment, option))
                }
            ) {
                Text(text = stringResource(R.string.button_continue))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChargeDisplayPreview() {
    ChargePaymentOption(
        handleCancel = {},
        handleEvent = {},
        isPaymentPix = false
    )
}

@Composable
fun CancelButton(handler: (MainEvent) -> Unit) {
    OutlinedButton(
        onClick = { handler(MainEvent.OnCancelAction) },
        modifier = Modifier
            .padding(5.dp)
            .background(color = Color.Transparent)
    ) {
        Text(
            text = stringResource(R.string.cancel_operation),
            modifier = Modifier.padding(8.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}

@Composable
fun AssembleVoidTransactionList(state: MainState, handler: (MainEvent) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.80f)
            .padding(top = 15.dp, bottom = 15.dp),
        verticalArrangement = Arrangement.Top
    ) {
        itemsIndexed(items = state.transactionsList) { _, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clickable { handler(MainEvent.OnSelectTransaction(item)) },
            ) {
                Text(
                    text = "R$ " + item.amount,
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .align(Alignment.CenterVertically),
                    fontSize = 20.sp,
                    textAlign = TextAlign.Start
                )

                Text(
                    text = "${item.date} ${item.time}",
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .weight(1.5f)
                        .padding(end = 10.dp),
                    fontSize = 20.sp,
                    textAlign = TextAlign.End
                )
            }

            Divider(color = Color.Gray)
        }
    }
}

@Composable
fun AssembleBluetoothDevicesList(state: MainState, handler: (MainEvent) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.80f)
            .padding(top = 15.dp, bottom = 15.dp),
        verticalArrangement = Arrangement.Top
    ) {
        itemsIndexed(items = state.bluetoothDevices) { _, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clickable { handler(MainEvent.OnSelectBtDevice(item)) },
            ) {
                Text(
                    text = item.name,
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .align(Alignment.CenterVertically),
                    fontSize = 20.sp,
                    textAlign = TextAlign.Start
                )

                Text(
                    text = item.address,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .weight(1.5f)
                        .padding(end = 10.dp),
                    fontSize = 20.sp,
                    textAlign = TextAlign.End
                )
            }

            Divider(color = Color.Gray)
        }
    }
}


