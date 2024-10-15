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
    val bluetoothDevices: List<BluetoothDevice> = listOf()
)

enum class Status {
    MESSAGE,
    QR_CODE,
    DISPLAY_VOID_LIST,
    DISPLAY_BLUETOOTH_LIST,
    FINISHED,
    DISPLAY_OPTION_PAYMENT,
    NONE
}
