package com.example.mpos

import com.zoop.pos.collection.VoidTransaction
import com.zoop.pos.type.Option
import com.zoop.sdk.plugin.mpos.bluetooth.platform.BluetoothDevice

sealed class MainEvent {
    data class OnStartPayment(
        val amount: Long,
        val installment: Int,
        val option: Option
    ) : MainEvent()
    data class OnStartPix(val amount: Long) : MainEvent()
    data class OnSelectBtDevice(val device: BluetoothDevice) : MainEvent()
    data class OnSelectTransaction(val transaction: VoidTransaction) : MainEvent()
    data class OnWriteDisplay(val message: String) : MainEvent()
    data class OnSendSms(val phoneNumber: String) : MainEvent()

    object OnStartBluetooth : MainEvent()
    object OnStartLogin : MainEvent()
    object OnStartCancellation : MainEvent()
    object OnOptionPayment : MainEvent()
    object OnDisplayNone : MainEvent()
    object OnCancelAction : MainEvent()
    object OnTableLoad : MainEvent()
    object OnStartSms : MainEvent()
}