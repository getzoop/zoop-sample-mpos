package com.example.mpos

import com.zoop.pos.collection.TransactionData
import com.zoop.pos.collection.VoidTransaction
import com.zoop.pos.type.Option
import com.zoop.sdk.plugin.mpos.bluetooth.platform.BluetoothDevice

sealed class MainEvent {
    object OnStartBluetooth : MainEvent()
    object OnStartLogin : MainEvent()
    data class OnStartPayment(
        val amount: Long,
        val installment: Int,
        val option: Option
    ) : MainEvent()

    object OnOptionPayment : MainEvent()
    object OnDisplayNone : MainEvent()
    data class OnStartPix(val amount: Long) : MainEvent()
    object OnStartCancellation : MainEvent()
    object OnCancelAction : MainEvent()
    object TableLoad : MainEvent()
    class OnSelectTransaction(val transaction: TransactionData) : MainEvent()
    class OnSelectBtDevice(val device: BluetoothDevice) : MainEvent()
    class OnWriteDisplay(val message: String) : MainEvent()
}