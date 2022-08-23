//
//  ParticlePeripheral.swift
//  BP monitor app
//
//  Created by fueb on 05/07/22.
//  Copyright © 2022 fueb. All rights reserved.
//

import UIKit
import CoreBluetooth

class ParticlePeripheral: NSObject {

    public static let BLEServiceUUID = CBUUID.init(string:"0000ffe0-0000-1000-8000-00805f9b34fb")
    public static let BLECharacteristicUUID = CBUUID.init(string: "0000ffe1-0000-1000-8000-00805f9b34fb")
}
