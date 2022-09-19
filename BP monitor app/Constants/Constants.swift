//
//  Constants.swift
//  BP monitor app
//
//  Created by fueb on 05/07/22.
//  Copyright © 2022 fueb. All rights reserved.
//

import UIKit

class Constants: NSObject {

    //Write commands to ble
    var deviceId:[UInt8] = [0x00,0x00,0x00,0x01]
    var startValue:[UInt8] = [0x7B,0x00,0x00,0x00,0x01,0x10,0x0A,0x00,0x01,0x00,0x1C,0x7D]
    var checkSumError:[UInt8] = [0x7B,0x00,0x00,0x00,0x01,0x14,0x0A,0x00,0x0E,0x00,0x1C,0x7D]
    var ack:[UInt8] = [0x7B,0x00,0x00,0x00,0x01,0x14,0x0A,0x00,0x0A,0x00,0x1C,0x7D]
    var noAck:[UInt8] = [0x7B,0x00,0x00,0x00,0x01,0x14,0x0A,0x00,0x00,0x00,0x1C,0x7D]
    var cancelValue:[UInt8] = [0x7B,0x00,0x00,0x00,0x01,0x10,0x0A,0x00,0x02,0x00,0x1C,0x7D]
    var resetValue:[UInt8] = [0x7B,0x00,0x00,0x00,0x01,0x02,0x0A,0x00,0x01,0x00,0x1C,0x7D]
    var noResetValue:[UInt8] = [0x7B,0x00,0x00,0x00,0x01,0x02,0x0A,0x00,0x00,0x00,0x1C,0x7D]
    
    //Command ids
    let RAW_COMMANDID:UInt8 = 17
    let RESULT_COMMANDID:UInt8 = 18
    let ERROR_COMMANDID:UInt8 = 19
    let ACK_COMMANDID:UInt8 = 20
    let DEVICE_COMMANDID:UInt8 = 01
    let BATTERY_COMMANDID:UInt8 = 21
    
    //Battery status
    let HIGH_BATTERY:UInt8 = 51
    let MID_BATTERY:UInt8 = 34
    let LOW_BATTERY:UInt8 = 17
    
    //Flags
    var is_ackReceived = false
    var is_finalResultReceived = false
    var is_rawResultReceived = false
    var is_errorReceived = false
    var is_battery_received = false
    var is_batterystatus = false
    var is_startTapped = false
    var is_stopTapped = false
    var is_ackInCuff = false
    var is_ackInBattery = false
    var is_poweroff = false
    var cuffPop = true
    var heartbeatPop = true
    var batteryPop = true
    var is_deviceReceived = false
    
    override init() {
        super .init()
       
    }

}
