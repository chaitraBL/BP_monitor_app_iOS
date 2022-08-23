//
//  BLEManager.swift
//  BP monitor app
//
//  Created by fueb on 05/07/22.
//  Copyright © 2022 fueb. All rights reserved.
//https://diamantidis.github.io/2020/02/16/broadcast-and-scan-nearby-devices-using-corebluetooth
//https://www.novelbits.io/intro-ble-mobile-development-ios-part-2/

import UIKit
import CoreBluetooth

class BLEManager: NSObject {
 
    var constantVal = Constants()
    
    override init() {
        super .init()
       
//        centralManager.delegate = self
    }
    
    func checkSumValidation(data:[UInt16], characteristics:CBCharacteristic) -> Bool {
        var checkSum:UInt16 = 0
        var checksumVerified:UInt16 = 0
        
        if data[0] == 123 || data[0] == 91 || data[0] == 40{
            
            switch data[5] {
            case UInt16(constantVal.DEVICE_COMMANDID):
                checkSum = data[8] * 256 + data[9]
                break
                
            case UInt16(constantVal.RAW_COMMANDID):
                checkSum = data[12] * 256 + data[13]
                break
                
            case UInt16(constantVal.RESULT_COMMANDID):
                checkSum = data[14] * 256 + data[15]
                break
                
            case UInt16(constantVal.ERROR_COMMANDID):
                checkSum = data[9] * 256 + data[10]
                break
                
            case UInt16(constantVal.ACK_COMMANDID):
                checkSum = data[9] * 256 + data[10]
                break
                
            case UInt16(constantVal.BATTERY_COMMANDID):
                checkSum = data[9] * 256 + data[10]
                break
                
            default:
                print("No commandId found")
                checkSum = data[9] * 256 + data[10]
                break
                
            }
            
//            print("check sum compute \(checkSum)")
        
        let length:UInt16 = data[6]
            for i:UInt16 in 1...length - 2 {
                checksumVerified += data[Int(i)]
            }
        }
        
//        print("checksum verified \(checksumVerified)")
        if checkSum == checksumVerified {
            return true
        }
        else {
            return false
        }
    }
    
    func computeCheckSum(data:[UInt8]) -> [UInt8] {
        var value = data
        let length = value[6]
        var final_checksum = 0
        
//        print("length \(length)")
        
        for i in 1...length - 2 {
//            print("check sum in loop \(value)")
            final_checksum += Int(value[Int(i)])
            
        }
//        print("final checksum \(final_checksum)")
        
        value[9] = UInt8(final_checksum >> 8)
        value[10] = UInt8(final_checksum)
//        print("check sum \(value[9]) & \(value[10])")
        return value
        
    }
    
    func replaceDeviceVal(value:[UInt8], value1:[UInt8]) -> [UInt8] {
        var data = value
  
        data[1] = value1[0]
        data[2] = value1[1]
        data[3] = value1[2]
        data[4] = value1[3]
        return data
    }
    
    // Calculate MAP
    func calculateMap(systa:Int, diasta:Int) -> Int {
        var map = 0
        map = (systa + 2 * diasta) / 3
        return map
    }
}

struct Peripheral {
    let id:Int
    let name:String
    let rssi:Int
}
