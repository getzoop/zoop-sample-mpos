package com.example.mpos

import com.zoop.pos.collection.VoidTransaction
import com.zoop.sdk.plugin.mpos.bluetooth.platform.BluetoothDevice

data class MainState(
    val status: Status = Status.FINISHED,
    val startPayment: Boolean = false,
    val startLogin: Boolean = false,
    val message: String = "",
    val qrCode: String = "",
    val transactionsList: List<VoidTransaction> = listOf(),
    val bluetoothDevices: List<BluetoothDevice> = listOf(),
)

enum class Status {
    NONE,
    ASK_PHONE_NUMBER,
    DISPLAY_BLUETOOTH_LIST,
    DISPLAY_OPTION_PAYMENT,
    DISPLAY_VOID_LIST,
    FINISHED,
    MESSAGE,
    QR_CODE,
    SENDING_SMS,
}
