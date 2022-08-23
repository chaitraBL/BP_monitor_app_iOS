//
//  DataReadingViewController.swift
//  BP monitor app
//
//  Created by fueb on 28/07/22.
//  Copyright © 2022 fueb. All rights reserved.
//

import UIKit
import CoreBluetooth

class DataReadingViewController: UIViewController {

    
    @IBOutlet weak var imgView: UIImageView!
    var indexVal = 0
    @IBOutlet weak var statusLabel: UILabel!
    @IBOutlet weak var readingsLabel: UILabel!

    @IBOutlet weak var systolicLabel: UILabel!
    @IBOutlet weak var batteryLabel: UILabel!
  
    var connStatus:String = ""
    @IBOutlet weak var mapLabel: UILabel!
    @IBOutlet weak var heartRateLabel: UILabel!
    
    @IBOutlet var stopBtn: UIButton!
    @IBOutlet var startbtn: UIButton!
    @IBOutlet var diastolicLab: UILabel!
    var periperalData:CBPeripheral!
    var centralManager:CBCentralManager!
    private var mNotifyCharacteristics:CBCharacteristic!
    var constantValue = Constants()
    var batteryVal = 0
    var cuffval:UInt16 = 0
    var pulseVal:UInt16 = 0
    var systolicVal:UInt16 = 0
    var diastolicVal:UInt16 = 0
    var heartRateVal:UInt16 = 0
    var map:UInt16 = 0
    var localDB = LocalNetworking()
    var bleManagerReading = BLEManager()
    var peripheralManager:CBPeripheralManager!
    var secondsRemaining = 2
    @IBOutlet var activityView: UIView!
    
    @IBOutlet var activityIndicator: UIActivityIndicatorView!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        dataTransferClass = true
        self.navigationController?.navigationItem.hidesBackButton = true
        navigationItem.setHidesBackButton(true, animated: false)
        self.tabBarController?.tabBar.isHidden = true
        drawArc()
        stopBtn.isHidden = true
        startbtn.isHidden = false
        stopBtn.isEnabled = false
        startbtn.isEnabled = true
//        print("index \(indexVal)")
    
        centralManagerInAppdelegate = centralManager
        
        periperalData.delegate = self
        connStatus = decodePeripheralState(peripheralState: periperalData.state, peripheral: periperalData)
        statusLabel.text = connStatus
        peripheralManager = CBPeripheralManager(delegate: self, queue: nil)
        peripheralInAppdelegate = periperalData
        
        startbtn.layer.cornerRadius = 0.5 * startbtn.bounds.size.width
        startbtn.clipsToBounds = true
        
        stopBtn.layer.cornerRadius = 0.5 * stopBtn.bounds.size.width
        stopBtn.clipsToBounds = true
        
        activityView.isHidden = false
        activityIndicator.startAnimating()
        constantValue.is_battery_received = false
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        centralManagerInAppdelegate = centralManager
        peripheralManager = CBPeripheralManager(delegate: self, queue: nil)
        peripheralInAppdelegate = periperalData
        constantValue.is_battery_received = false
        connStatus = decodePeripheralState(peripheralState: periperalData.state, peripheral: periperalData)
        statusLabel.text = connStatus
        
        UIApplication.shared.isIdleTimerDisabled = true
        
        while (false) {
            connStatus = decodePeripheralState(peripheralState: periperalData.state, peripheral: periperalData)
            statusLabel.text = connStatus
        }
    }
    
// https://www.ioscreator.com/tutorials/draw-shapes-core-graphics-ios-tutorial - to Draw different shapes.
    private func drawArc() {
        let renderer = UIGraphicsImageRenderer(size: CGSize(width: 300, height: 270))
        
        let img = renderer.image { ctx in
            let rect = CGRect(x: 5, y: 5, width: 290, height: 250)
            
            // 6
            ctx.cgContext.setFillColor(UIColor.white.cgColor)
            ctx.cgContext.setStrokeColor(UIColor.black.cgColor)
            ctx.cgContext.setLineWidth(5)
            
            ctx.cgContext.addEllipse(in: rect)
            ctx.cgContext.drawPath(using: .fillStroke)
        }
        
        imgView.image = img
    }
    
    
    @IBAction func disableBluetooth(_ sender: UIBarButtonItem) {
        let alert = UIAlertController(title: "Alert!!!", message: "Are you sure, do you want to disconnect.", preferredStyle: .alert)

        alert.addAction(UIAlertAction(title: "OK", style: .default, handler: { [self] action in
            centralManager.cancelPeripheralConnection(periperalData)
            periperalData = nil
            performSegue(withIdentifier: "disconnectPeripheral", sender: self)
        }))
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel, handler: nil))
        self.present(alert, animated: true)
    }
    
override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
    if (segue.identifier == "disconnectPeripheral") {
        self.tabBarController?.tabBar.isHidden = false
    }
}
  
    @IBAction func startReading(_ sender: Any) {
        
        constantValue.is_startTapped = true
        constantValue.is_stopTapped = false
//        constantValue.is_battery_received = false
        constantValue.is_irregularHB = false
        constantValue.is_cuffReplaced = false
        constantValue.is_ackReceived = false
        constantValue.is_finalResultReceived = false
        constantValue.is_ackReceived = false
        constantValue.is_errorReceived = false
        constantValue.is_rawResultReceived = false
        constantValue.is_batterystatus = false
        
        systolicLabel.text = "-"
        diastolicLab.text = "-"
        heartRateLabel.text = "-"
        mapLabel.text = "-"
        
        constantValue.startValue = bleManagerReading.computeCheckSum(data: constantValue.startValue)
//        print("checksum updated \(constantValue.startValue)")
        writeOutGoingValue(data: constantValue.startValue)
        
    }
    
    @IBAction func stopReading(_ sender: UIButton) {
        constantValue.is_startTapped = false
        constantValue.is_stopTapped = true
        
        constantValue.is_irregularHB = false
        constantValue.is_cuffReplaced = false
        constantValue.is_ackReceived = false
        constantValue.is_finalResultReceived = false
        constantValue.is_ackReceived = false
        constantValue.is_errorReceived = false
        constantValue.is_rawResultReceived = false
        constantValue.is_batterystatus = false
        
        constantValue.cancelValue = bleManagerReading.computeCheckSum(data: constantValue.cancelValue)
//        print("checksum updated \(constantValue.cancelValue)")
        writeOutGoingValue(data: constantValue.cancelValue)
        
        if constantValue.is_finalResultReceived == true {
            systolicLabel.text = "\(systolicVal)"
            diastolicLab.text = "\(diastolicVal)"
            heartRateLabel.text = "\(heartRateVal)"
            mapLabel.text = "\(map)"
        }
        else {
            systolicLabel.text = "-"
            diastolicLab.text = "-"
            heartRateLabel.text = "-"
            mapLabel.text = "-"
        }
    }
    
    @IBAction func saveToLocal(_ sender: UIButton) {
        activityView.isHidden = false
        activityIndicator.startAnimating()
        
        if ((Int(systolicLabel.text!)! < 30) || (Int(systolicLabel.text!)! > 200)) {
            activityView.isHidden = true
            activityIndicator.stopAnimating()
            let alert = UIAlertController(title: "Alert!", message: "Systolic range should be between 30 to 200,\n Please check the readings...", preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: "OK", style: .default, handler: nil))
            self.present(alert, animated: true)
        }
        else if ((Int(diastolicLab.text!)! < 40) || (Int(diastolicLab.text!)! > 120)) {
            activityView.isHidden = true
            activityIndicator.stopAnimating()
            let alert = UIAlertController(title: "Alert!", message: "Diastolic range should be between 40 to 120,\n Please check the readings...", preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: "OK", style: .default, handler: nil))
            self.present(alert, animated: true)
        }
        else if ((systolicLabel.text == "-") || (diastolicLab.text == "-") || (heartRateLabel.text == "-") || (mapLabel.text == "-")) {
            activityView.isHidden = true
            activityIndicator.stopAnimating()
            let alert = UIAlertController(title: "Alert!", message: "Please check the reading value...", preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: "OK", style: .default, handler: nil))
            self.present(alert, animated: true)
        }
        else {
            activityView.isHidden = true
            activityIndicator.stopAnimating()
            print("peripheral name = \(String(describing: periperalData.name))")
            let isSuccess = localDB.save(name: periperalData.name!, systolic: systolicLabel.text!, diastolic: diastolicLab.text!, heartRate: heartRateLabel.text!, map: mapLabel.text!)
            
            if isSuccess == true {
                let alert = UIAlertController(title: "Success", message: "Saved successfully", preferredStyle: .alert)
                alert.addAction(UIAlertAction(title: "OK", style: .default, handler: nil))
                self.present(alert, animated: true)
                systolicLabel.text = "-"
                diastolicLab.text = "-"
                heartRateLabel.text = "-"
                mapLabel.text = "-"
            }
            else {
                let alert = UIAlertController(title: "Unsuccess", message: "Failed to save", preferredStyle: .alert)
                alert.addAction(UIAlertAction(title: "OK", style: .default, handler: nil))
                self.present(alert, animated: true)
            }
        }
    }
    
    func showBattery() {
        if batteryVal == constantValue.HIGH_BATTERY {
            activityView.isHidden = true
            activityIndicator.stopAnimating()
            batteryLabel.backgroundColor = UIColor(hexString: "#008000")
            
            constantValue.ack = bleManagerReading.computeCheckSum(data: constantValue.ack)
//            print("checksum updated \(constantValue.ack)")
            writeOutGoingValue(data: constantValue.ack)
        }
        else if batteryVal == constantValue.MID_BATTERY {
            activityView.isHidden = true
            activityIndicator.stopAnimating()
            batteryLabel.backgroundColor = UIColor(hexString: "#FFA500")
            
            constantValue.ack = bleManagerReading.computeCheckSum(data: constantValue.ack)
//            print("checksum updated \(constantValue.ack)")
            writeOutGoingValue(data: constantValue.ack)
            
        }
        else if batteryVal == constantValue.LOW_BATTERY {
            activityView.isHidden = true
            activityIndicator.stopAnimating()
            batteryLabel.backgroundColor = UIColor(hexString: "#FF0000")
            showToast(message: "Battery is low, Please charge and reconnect", font: .systemFont(ofSize: 12))
            
            constantValue.ack = bleManagerReading.computeCheckSum(data: constantValue.ack)
//            print("checksum updated \(constantValue.ack)")
            writeOutGoingValue(data: constantValue.ack)
            
        }
        else{
            activityView.isHidden = true
            activityIndicator.stopAnimating()
            batteryLabel.backgroundColor = UIColor(hexString: "#a41e22")
//            showToast(message: "Battery level exceeded, Please change the battery", font: .systemFont(ofSize: 12))
            
            let alert = UIAlertController(title: "Alert!!!", message: "Battery level exceeded, Please change the battery", preferredStyle: .alert)

            alert.addAction(UIAlertAction(title: "OK", style: .default, handler: { [self] action in
                self.constantValue.ack = self.bleManagerReading.computeCheckSum(data: self.constantValue.ack)
//                print("checksum updated \(self.constantValue.ack)")
                writeOutGoingValue(data: self.constantValue.ack)
            }))
            self.present(alert, animated: true)
        }
    }
    
}

extension DataReadingViewController: CBPeripheralDelegate {
  
        func decodePeripheralState(peripheralState: CBPeripheralState, peripheral:CBPeripheral) -> String{
            var msg:String = ""
                
                switch peripheralState {
                    case .connecting:
//                        print("Peripheral state: connecting")
                        msg = "Connecting"
                    break
                    
                    case .disconnecting:
//                        print("Peripheral state: disconnecting")
                        msg = "Disconnecting"
                    break
                    
                    case .disconnected:
//                        print("Peripheral state: disconnected")
                        msg = "Disconnected"
                    centralManager.cancelPeripheralConnection(periperalData)
                    periperalData = nil
                    
                    let alert = UIAlertController(title: "Alert!!!", message: "Disconnected, please try again", preferredStyle: .alert)

                    alert.addAction(UIAlertAction(title: "OK", style: .default, handler: { action in
                       
                        self.performSegue(withIdentifier: "disconnectPeripheral", sender: self)
                        alert.dismiss(animated: false)
                        
                    }))
                    self.present(alert, animated: true)
                    
                    break
                    
                    case .connected:
//                        print("Peripheral state: connected")
                        msg = "Connected"
                    break
                }
            return msg
        }
    

       
        func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
            if ((error) != nil) {
                print("Error discovering services: \(error!.localizedDescription)")
                return
            }
//            guard let services = peripheral.services else {return}
            for service:CBService in peripheral.services! {
//                print("Services \(service)")
                peripheral.discoverCharacteristics(nil, for: service)
            }
//            if periperalData.state == .disconnected {
//                centralManager.cancelPeripheralConnection(periperalData)
//                periperalData = nil
//                connStatus = decodePeripheralState(peripheralState: periperalData.state, peripheral: periperalData)
//                statusLabel.text = connStatus
//                print("Disconnected")
//            }
    //        print("Services list \(services)")
        }
        
        func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor service: CBService, error: Error?) {
            guard let characteristic = service.characteristics else { return}
            
            for characteristics:CBCharacteristic in characteristic {
               
                if characteristics.uuid.isEqual(ParticlePeripheral.BLECharacteristicUUID) {
                    print("Characteristics: \(characteristics)")
                    
                    mNotifyCharacteristics = characteristics
                    peripheral.setNotifyValue(true, for: characteristics)
                    peripheral.readValue(for: characteristics)
//                    print("read value \( peripheral.readValue(for: characteristics))")
                    
                }
            }
//            if periperalData.state == .disconnected {
//                centralManager.cancelPeripheralConnection(periperalData)
//                periperalData = nil
//                connStatus = decodePeripheralState(peripheralState: periperalData.state, peripheral: periperalData)
//                statusLabel.text = connStatus
//                print("Disconnected")
//            }
        }
        
        func peripheral(_ peripheral: CBPeripheral, didUpdateNotificationStateFor characteristic: CBCharacteristic, error: Error?) {
            if (error != nil) {
                print("error reading characteristics \(String(describing: error?.localizedDescription))")
            }
            if (characteristic.isNotifying) {
                print("notification began \(characteristic)")
            }
//            if periperalData.state == .disconnected {
//                centralManager.cancelPeripheralConnection(periperalData)
//                periperalData = nil
//                connStatus = decodePeripheralState(peripheralState: periperalData.state, peripheral: periperalData)
//                statusLabel.text = connStatus
//                print("Disconnected")
//            }
        }
        
        func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
            if characteristic.uuid == ParticlePeripheral.BLECharacteristicUUID {
                print("characteristc value \(characteristic)")
                if (error != nil) {
                    print("error characteristics \(String(describing: error?.localizedDescription))")
                }
                if characteristic != nil {
                            if characteristic.value != nil {
                               
                                self.broadcastUpdate(characteristic: characteristic)
                            }
                            else {
                                print("empty")
//                                self.showToast(message: "Something went wrong, please try to reconnect again", font: .systemFont(ofSize: 12))
                            }
                }
            }
        }
        
        func broadcastUpdate(characteristic:CBCharacteristic)  {
            self.secondsRemaining = 2
            guard let characteristicData = characteristic.value else {return}
            print("character data: \(characteristic)")
            let byteArray = [UInt8](characteristicData)
            
//            print("byte array \(byteArray) ")
//            print("hex array: \(byteArray.bytesToHex(spacing: " "))")
            if byteArray[0] == 123 || byteArray[0] == 91 || byteArray[0] == 40 {
                
                print("command id \(byteArray[5])")
                
                let newArray = byteArray.map { UInt16($0) }

    //            print("new array \(newArray)")
                let verified = bleManagerReading.checkSumValidation(data: newArray, characteristics: characteristic)
                print("checksum verified \(verified)")
                
                if verified == true {
                    
                    Timer.scheduledTimer(withTimeInterval: 0.09, repeats: true) { (Timer) in //0.1 /0.09, false
                            if self.secondsRemaining > 0 {
                                print ("\(self.secondsRemaining) seconds")
//                                Timer.invalidate()
//                                self.secondsRemaining = 3
                    
                                switch byteArray[5] {
                                case self.constantValue.DEVICE_COMMANDID:
                                    print("device id: \(byteArray)")
                    
                                    Timer.invalidate()
                                    self.secondsRemaining = 2
//                                    Timer = nil
                                    self.constantValue.deviceId.remove(at: 0)
                                    self.constantValue.deviceId.insert(byteArray[1], at: 0)
                                    self.constantValue.deviceId.remove(at: 1)
                                    self.constantValue.deviceId.insert(byteArray[2], at: 1)
                                    self.constantValue.deviceId.remove(at: 2)
                                    self.constantValue.deviceId.insert(byteArray[3], at: 2)
                                    self.constantValue.deviceId.remove(at: 3)
                                    self.constantValue.deviceId.insert(byteArray[4], at: 3)
                    
                    
                    //                    print("new device id \(constantValue.deviceId)")
                    
                                    self.constantValue.startValue = self.bleManagerReading.replaceDeviceVal(value: self.constantValue.startValue, value1: self.constantValue.deviceId)
                    //                print("new start value \(constantValue.startValue)")
                                    self.constantValue.ack = self.bleManagerReading.replaceDeviceVal(value: self.constantValue.ack, value1: self.constantValue.deviceId)
                                    self.constantValue.noAck = self.bleManagerReading.replaceDeviceVal(value: self.constantValue.noAck, value1: self.constantValue.deviceId)
                                    self.constantValue.resetValue = self.bleManagerReading.replaceDeviceVal(value: self.constantValue.resetValue, value1: self.constantValue.deviceId)
                                    self.constantValue.noResetValue = self.bleManagerReading.replaceDeviceVal(value: self.constantValue.noResetValue, value1: self.constantValue.deviceId)
                                    self.constantValue.checkSumError = self.bleManagerReading.replaceDeviceVal(value: self.constantValue.checkSumError, value1: self.constantValue.deviceId)
                                    break
                    
                                case self.constantValue.BATTERY_COMMANDID:
                                    Timer.invalidate()
                                    self.secondsRemaining = 2
                                    print("battery: \(byteArray)")
                                    self.constantValue.is_battery_received = true
                                    self.batteryVal = Int(byteArray[8])
                    //                    print("battery value: \(batteryVal)")
                                    self.showBattery()
                    
                                    break
                    
                                case self.constantValue.RAW_COMMANDID:
                                    Timer.invalidate()
                                    self.secondsRemaining = 2
                    //                        print("raw value: \(byteArray)")
                    //                    let readArray = byteArray.map { UInt16($0) }
                                    self.constantValue.is_rawResultReceived = true
                                    self.cuffval = newArray[8] * 256 + newArray[9]
                                    print("cuff value \(self.cuffval)")
                                    self.pulseVal = newArray[10] * 256 + newArray[11]
                                    print("pulse value \(self.pulseVal)")
                    
                    //                        if constantValue.is_ackReceived == true {
                                    self.readingsLabel.text = "\(self.cuffval)" + " / " + "\(self.pulseVal)"
                    //                                    self.constantValue.is_rawResultReceived = false
                    //                        }
                    
                                    break
                    
                                case self.constantValue.RESULT_COMMANDID:
                                    print("result value: \(byteArray)")
                                    
                                    Timer.invalidate()
                                    self.secondsRemaining = 2
                                    self.constantValue.is_finalResultReceived = true
                    //                                    self.constantValue.is_rawResultReceived = false
                                    
                                    self.systolicVal = newArray[8] * 256 + newArray[9]
                                    self.diastolicVal = newArray[10] * 256 + newArray[11]
                                    self.heartRateVal = newArray[12]
                                    self.map = newArray[13]
                    
                                    print("final result: \(self.systolicVal) / \(self.diastolicVal) / \(self.heartRateVal) / \(self.map)")
                    
                                    self.readingsLabel.text = "\(self.systolicVal) / \(self.diastolicVal) / \(self.heartRateVal)"
                    
                                    self.constantValue.ack = self.bleManagerReading.computeCheckSum(data: self.constantValue.ack)
                        //            print("checksum updated \(constantValue.ack)")
                                    self.writeOutGoingValue(data: self.constantValue.ack)
                    
                                    break
                    
                                case self.constantValue.ERROR_COMMANDID:
                                    print("error value: \(byteArray)")
//                                    Timer.invalidate()
//                                    self.secondsRemaining = 2
                                    let errorVal = byteArray[8]
                                    var msg = ""
                                    switch errorVal {
                                    case 1:
                                        Timer.invalidate()
                                        self.secondsRemaining = 2
                                        self.constantValue.is_errorReceived = true
                                        msg = "Cuff placement/fitment incorrect, Please change & try again"
                                        self.readingsLabel.text = msg
                                        self.constantValue.ack = self.bleManagerReading.computeCheckSum(data: self.constantValue.ack)
                            //            print("checksum updated \(constantValue.ack)")
                                        self.writeOutGoingValue(data: self.constantValue.ack)
                
                                        break
                    
                                    case 2:
                                        Timer.invalidate()
                                        self.secondsRemaining = 2
                                        self.constantValue.is_errorReceived = true
                                        msg = "Hand movement detected, Please try again"
                                        self.readingsLabel.text = msg
                                        self.constantValue.ack = self.bleManagerReading.computeCheckSum(data: self.constantValue.ack)
                            //            print("checksum updated \(constantValue.ack)")
                                        self.writeOutGoingValue(data: self.constantValue.ack)
                                        break
                    
                                    case 3:
                                        Timer.invalidate()
                                        self.secondsRemaining = 2
                                        self.constantValue.is_irregularHB = true
                                        msg = "Irregular heartbeat detected, Please try again"
                                        self.readingsLabel.text = "---"
                    
                                        let alert = UIAlertController(title: "Alert!!!", message: msg, preferredStyle: .alert)
                    
                                        alert.addAction(UIAlertAction(title: "OK", style: .default, handler: { [self] action in
                                            
//                                            self.constantValue.ack = self.bleManagerReading.computeCheckSum(data: self.constantValue.ack)
//                                //            print("checksum updated \(constantValue.ack)")
//                                            self.writeOutGoingValue(data: self.constantValue.ack)
                                            self.constantValue.is_ackInIrregularHB = true
                                            self.constantValue.cancelValue = self.bleManagerReading.computeCheckSum(data: self.constantValue.cancelValue)
                            //                print("checksum updated \(self.constantValue.ack)")
                                            writeOutGoingValue(data: self.constantValue.cancelValue)
                    //                                readingsLabel.text = "---"
                                           
                                        }))
                                        self.present(alert, animated: true)
                    
                                        break
                    
                                    case 4:
                                        Timer.invalidate()
                                        self.secondsRemaining = 2
                                        self.constantValue.is_errorReceived = true
                                        msg = "Cuff over pressurised, Please try again"
                                        self.readingsLabel.text = msg
                                        self.constantValue.ack = self.bleManagerReading.computeCheckSum(data: self.constantValue.ack)
                            //            print("checksum updated \(constantValue.ack)")
                                        self.writeOutGoingValue(data: self.constantValue.ack)
                                        break
                    
                                    case 5:
                                        Timer.invalidate()
                                        self.secondsRemaining = 2
                                        msg = "Low battery, Please charge the batteries"
                                        self.batteryLabel.backgroundColor = UIColor(hexString: "#FF0000")
                                        self.readingsLabel.text = "---"
                                        let alert = UIAlertController(title: "Alert!!!", message: msg, preferredStyle: .alert)
                    
                                        alert.addAction(UIAlertAction(title: "OK", style: .default, handler: { [self] action in
                                            self.constantValue.is_ackInBattery = true
                                            self.constantValue.cancelValue = self.bleManagerReading.computeCheckSum(data: self.constantValue.cancelValue)
                            //                print("checksum updated \(self.constantValue.ack)")
                                            writeOutGoingValue(data: self.constantValue.cancelValue)
//                                            stopBtn.isHidden = true
//                                            startbtn.isHidden = false
//                                            stopBtn.isEnabled = false
//                                            startbtn.isEnabled = true
                    //                                readingsLabel.text = "---"
                                        }))
                                        self.present(alert, animated: true)
                    
                                        break
                    
                                    case 6:
                                        self.constantValue.is_cuffReplaced = true
                                        Timer.invalidate()
                                        self.secondsRemaining = 2
                                        msg = "Please replace to new cuff!!!"
                                        self.readingsLabel.text = "---"
                    
                                        let alert = UIAlertController(title: "Alert!!!", message: msg, preferredStyle: .alert)
                    
                                        alert.addAction(UIAlertAction(title: "Reset", style: .default, handler: { [self] action in
                                            self.constantValue.is_ackInCuff = true
                                            self.constantValue.resetValue = self.bleManagerReading.computeCheckSum(data: self.constantValue.resetValue)
                            //                print("checksum updated \(self.constantValue.ack)")
                                            self.writeOutGoingValue(data: self.constantValue.resetValue)
                    //                                            self.constantValue.is_cuffReplaced = false
                    //                                readingsLabel.text = "---"
                                        }))
                                        alert.addAction(UIAlertAction(title: "Cancel", style: .default, handler: { action in
                                            self.constantValue.is_ackInCuff = true
                                            self.constantValue.noResetValue = self.bleManagerReading.computeCheckSum(data: self.constantValue.noResetValue)
                            //                print("checksum updated \(self.constantValue.ack)")
                                            self.writeOutGoingValue(data: self.constantValue.noResetValue)
                    //                                            self.constantValue.is_cuffReplaced = false
                                        }))
                                        self.present(alert, animated: true)
                                        //reset/ noreset
                                        break
                    
                                    case 7:
                                        self.constantValue.is_irregularHB = true
                                        Timer.invalidate()
                                        self.secondsRemaining = 2
                                        msg = "Heartbeat varied please retry again"
                                        self.readingsLabel.text = "---"
                                        let alert = UIAlertController(title: "Alert!!!", message: msg, preferredStyle: .alert)
                    
                                        alert.addAction(UIAlertAction(title: "OK", style: .default, handler: { [self] action in
                                            
//                                            self.constantValue.ack = self.bleManagerReading.computeCheckSum(data: self.constantValue.ack)
//                                //            print("checksum updated \(constantValue.ack)")
//                                            self.writeOutGoingValue(data: self.constantValue.ack)
                                            self.constantValue.is_ackInIrregularHB = true
                                            self.constantValue.cancelValue = self.bleManagerReading.computeCheckSum(data: self.constantValue.cancelValue)
                            //                print("checksum updated \(self.constantValue.ack)")
                                            writeOutGoingValue(data: self.constantValue.cancelValue)
                                            
                    //                                readingsLabel.text = "---"
                    //                                            self.constantValue.is_irregularHB = false
                                        }))
                                        self.present(alert, animated: true)
                    
                                        break
                    
                                    case 8:
                                        self.constantValue.is_batterystatus = true
                                        Timer.invalidate()
                                        self.secondsRemaining = 2
                                        msg = "Battery level exceeded, Please change the battery"
                    
                                        self.batteryLabel.backgroundColor = UIColor(hexString: "#a41e22")
                                        self.readingsLabel.text = "---"
                                        let alert = UIAlertController(title: "Alert!!!", message: "Battery level exceeded, Please change the battery", preferredStyle: .alert)
                    
                                        alert.addAction(UIAlertAction(title: "OK", style: .default, handler: { [self] action in
                                            self.constantValue.is_ackInBattery = true
                                            self.constantValue.cancelValue = self.bleManagerReading.computeCheckSum(data: self.constantValue.cancelValue)
                            //                print("checksum updated \(self.constantValue.ack)")
                                            writeOutGoingValue(data: self.constantValue.cancelValue)
//                                            stopBtn.isHidden = true
//                                            startbtn.isHidden = false
//                                            stopBtn.isEnabled = false
//                                            startbtn.isEnabled = true
                    //                                            self.constantValue.is_batterystatus = false
                    //                                readingsLabel.text = "---"
                                        }))
                                        self.present(alert, animated: true)
                    //
                                        break
                    
                                    case 11:
                                        self.constantValue.is_batterystatus = true
                                        Timer.invalidate()
                                        self.secondsRemaining = 2
                                        self.batteryLabel.backgroundColor = UIColor(hexString: "#FF0000")
                    //                                        self.constantValue.is_batterystatus = false
                                        break
                    
                                    default:
                                        break
                    
                                    }
                                    break
                    
                                case self.constantValue.ACK_COMMANDID:
                                    print("ack: \(byteArray)")
                                    Timer.invalidate()
                                    self.secondsRemaining = 2
                                    self.constantValue.is_ackReceived = true
                                    if self.constantValue.is_stopTapped == true {
                                        self.stopBtn.isHidden = true
                                        self.startbtn.isHidden = false
                                        self.stopBtn.isEnabled = false
                                        self.startbtn.isEnabled = true
                                        self.readingsLabel.text = "---"
                                        self.constantValue.is_stopTapped = false
                                    }
                                    else if self.constantValue.is_startTapped == true {
                                        self.stopBtn.isHidden = false
                                        self.startbtn.isHidden = true
                                        self.stopBtn.isEnabled = true
                                        self.startbtn.isEnabled = false
                                        self.constantValue.is_startTapped = false
                                    }
                                    else if self.constantValue.is_ackInCuff == true {
                                        self.stopBtn.isHidden = true
                                        self.startbtn.isHidden = false
                                        self.stopBtn.isEnabled = false
                                        self.startbtn.isEnabled = true
                                        self.readingsLabel.text = "---"
                                        self.constantValue.is_ackInCuff = false
                                    }
                                    else if self.constantValue.is_ackInIrregularHB == true {
                                        self.stopBtn.isHidden = true
                                        self.startbtn.isHidden = false
                                        self.stopBtn.isEnabled = false
                                        self.startbtn.isEnabled = true
                                        self.readingsLabel.text = "---"
                                        self.constantValue.is_ackInIrregularHB = false
                                    }
                                    else if self.constantValue.is_ackInBattery == true {
                                        self.stopBtn.isHidden = true
                                        self.startbtn.isHidden = false
                                        self.stopBtn.isEnabled = false
                                        self.startbtn.isEnabled = true
                                        self.readingsLabel.text = "---"
                                        self.constantValue.is_ackInBattery = false
                                    }
                                    else {
                                        self.stopBtn.isHidden = true
                                        self.startbtn.isHidden = false
                                        self.stopBtn.isEnabled = false
                                        self.startbtn.isEnabled = true
                                        self.readingsLabel.text = "---"
                                    }
                                    break
                    
                                default:
                                    break
                                }
                                self.secondsRemaining -= 1
                    //                                self.constantValue.is_ackReceived = false
                            } else if self.secondsRemaining == 0 {
                    
                                if self.constantValue.is_battery_received == false {
                                    Timer.invalidate()
                                    self.secondsRemaining = 2
                                        self.showToast(message: "Something went wrong, please try again", font: .systemFont(ofSize: 12))
                                }
                    
                                    else if self.constantValue.is_ackReceived == false {
                                        if self.constantValue.is_rawResultReceived == false {
                                            Timer.invalidate()
                                            self.secondsRemaining = 2
                                            self.showToast(message: "Please try again", font: .systemFont(ofSize: 12))
                                        }
                    
                                        else if self.constantValue.is_rawResultReceived == false {
                                            Timer.invalidate()
                                            self.secondsRemaining = 2
                                            self.showToast(message: "Please try again", font: .systemFont(ofSize: 12))
                                        }
                    
                                        else if self.constantValue.is_cuffReplaced == true {
                                            if self.constantValue.is_ackReceived == false {
                                                self.constantValue.cancelValue = self.bleManagerReading.computeCheckSum(data: self.constantValue.cancelValue)
                                        //        print("checksum updated \(constantValue.cancelValue)")
                                                self.writeOutGoingValue(data: self.constantValue.cancelValue)
                                                self.secondsRemaining = 2
                                            }
                                        }
                    
                                        else if self.constantValue.is_errorReceived == false {
                                            Timer.invalidate()
                                            self.secondsRemaining = 2
                                            self.showToast(message: "Please try again", font: .systemFont(ofSize: 12))
                                        }
                                    }
                            }
                        }
                }
                else {
                    self.constantValue.checkSumError = self.bleManagerReading.computeCheckSum(data: self.constantValue.checkSumError)
    //                print("checksum updated \(self.constantValue.checkSumError)")
                    writeOutGoingValue(data: self.constantValue.checkSumError)
                }
            }
        }
        
        func writeOutGoingValue(data:[UInt8]) {
            
            let data1 = Data(bytesNoCopy: UnsafeMutableRawPointer(mutating: data), count: data.count, deallocator: .none)
            print("data1: \(data1)")
            if periperalData != nil {
                if mNotifyCharacteristics != nil {
                    periperalData.writeValue(data1, for: mNotifyCharacteristics, type: CBCharacteristicWriteType.withResponse)
                }
            }
        }
}

extension DataReadingViewController:CBPeripheralManagerDelegate {
    func peripheralManagerDidUpdateState(_ peripheral: CBPeripheralManager) {
        print(peripheral.state)
        switch peripheral.state {
        case .poweredOn:
            print("Power on")
            return
        case .poweredOff:
            print("Power off")
            centralManager.cancelPeripheralConnection(periperalData)
            periperalData = nil
            connStatus = decodePeripheralState(peripheralState: periperalData.state, peripheral: periperalData)
            statusLabel.text = connStatus
            return
        default:
            print("Other state")
            return
        }
    }
}
    
//Convert UInt8 to hexdecimal.
extension Array where Element == UInt8 {
  func bytesToHex(spacing: String) -> String {
    var hexString: String = ""
    var count = self.count
    for byte in self
    {
        hexString.append(String(format:"%02X", byte))
        count = count - 1
        if count > 0
        {
            hexString.append(spacing)
        }
    }
    return hexString
}
    //Convert UInt8 to string.
//   if let string = String(bytes: byteArray, encoding: .utf8) {
//        print("string array: \(string)")
//        showToast(message: "Value: \(string)", font: .systemFont(ofSize: 12))
//   } else {
//        print("not a valid UTF-8 sequence")
//   }

}

//Timer.scheduledTimer(withTimeInterval: 1.0, repeats: false) { (Timer) in
//        if self.secondsRemaining > 0 {
//            print ("\(self.secondsRemaining) seconds")
//            Timer.invalidate()
//
//            switch byteArray[5] {
//            case self.constantValue.DEVICE_COMMANDID:
//                print("device id: \(byteArray)")
//
//                self.constantValue.deviceId.remove(at: 0)
//                self.constantValue.deviceId.insert(byteArray[1], at: 0)
//                self.constantValue.deviceId.remove(at: 1)
//                self.constantValue.deviceId.insert(byteArray[2], at: 1)
//                self.constantValue.deviceId.remove(at: 2)
//                self.constantValue.deviceId.insert(byteArray[3], at: 2)
//                self.constantValue.deviceId.remove(at: 3)
//                self.constantValue.deviceId.insert(byteArray[4], at: 3)
//
//
////                    print("new device id \(constantValue.deviceId)")
//
//                self.constantValue.startValue = self.bleManagerReading.replaceDeviceVal(value: self.constantValue.startValue, value1: self.constantValue.deviceId)
////                print("new start value \(constantValue.startValue)")
//                self.constantValue.ack = self.bleManagerReading.replaceDeviceVal(value: self.constantValue.ack, value1: self.constantValue.deviceId)
//                self.constantValue.noAck = self.bleManagerReading.replaceDeviceVal(value: self.constantValue.noAck, value1: self.constantValue.deviceId)
//                self.constantValue.resetValue = self.bleManagerReading.replaceDeviceVal(value: self.constantValue.resetValue, value1: self.constantValue.deviceId)
//                self.constantValue.noResetValue = self.bleManagerReading.replaceDeviceVal(value: self.constantValue.noResetValue, value1: self.constantValue.deviceId)
//                self.constantValue.checkSumError = self.bleManagerReading.replaceDeviceVal(value: self.constantValue.checkSumError, value1: self.constantValue.deviceId)
//                break
//
//            case self.constantValue.BATTERY_COMMANDID:
//                Timer.invalidate()
//                print("battery: \(byteArray)")
//                self.constantValue.is_battery_received = true
//                self.batteryVal = Int(byteArray[8])
////                    print("battery value: \(batteryVal)")
//                self.showBattery()
//
//                break
//
//            case self.constantValue.RAW_COMMANDID:
//                Timer.invalidate()
////                        print("raw value: \(byteArray)")
////                    let readArray = byteArray.map { UInt16($0) }
//                self.constantValue.is_rawResultReceived = true
//                self.cuffval = newArray[8] * 256 + newArray[9]
//                print("cuff value \(self.cuffval)")
//                self.pulseVal = newArray[10] * 256 + newArray[11]
//                print("pulse value \(self.pulseVal)")
//
////                        if constantValue.is_ackReceived == true {
//                self.readingsLabel.text = "\(self.cuffval)" + " / " + "\(self.pulseVal)"
////                                    self.constantValue.is_rawResultReceived = false
////                        }
//
//                break
//
//            case self.constantValue.RESULT_COMMANDID:
//                print("result value: \(byteArray)")
//                Timer.invalidate()
////                                    self.constantValue.is_rawResultReceived = false
//                self.constantValue.is_finalResultReceived = true
//                self.systolicVal = newArray[8] * 256 + newArray[9]
//                self.diastolicVal = newArray[10] * 256 + newArray[11]
//                self.heartRateVal = newArray[12]
//                self.map = newArray[13]
//
//                print("final result: \(self.systolicVal) / \(self.diastolicVal) / \(self.heartRateVal) / \(self.map)")
//
//                self.readingsLabel.text = "\(self.systolicVal) / \(self.diastolicVal) / \(self.heartRateVal)"
//
//                self.constantValue.ack = self.bleManagerReading.computeCheckSum(data: self.constantValue.ack)
//    //            print("checksum updated \(constantValue.ack)")
//                self.writeOutGoingValue(data: self.constantValue.ack)
//
////                                    self.constantValue.is_finalResultReceived = false
//
//                break
//
//            case self.constantValue.ERROR_COMMANDID:
//                print("error value: \(byteArray)")
//                Timer.invalidate()
//
//                let errorVal = byteArray[8]
//                var msg = ""
//                switch errorVal {
//                case 1:
//                    self.constantValue.is_errorReceived = true
//                    msg = "Cuff placement/fitment incorrect, Please change & try again"
//                    self.readingsLabel.text = msg
//                    self.constantValue.ack = self.bleManagerReading.computeCheckSum(data: self.constantValue.ack)
//        //            print("checksum updated \(constantValue.ack)")
//                    self.writeOutGoingValue(data: self.constantValue.ack)
////                                        self.constantValue.is_errorReceived = false
//                    break
//
//                case 2:
//                    self.constantValue.is_errorReceived = true
//                    msg = "Hand movement detected, Please try again"
//                    self.readingsLabel.text = msg
//                    self.constantValue.ack = self.bleManagerReading.computeCheckSum(data: self.constantValue.ack)
//        //            print("checksum updated \(constantValue.ack)")
//                    self.writeOutGoingValue(data: self.constantValue.ack)
////                                        self.constantValue.is_errorReceived = false
//                    break
//
//                case 3:
//                    self.constantValue.is_irregularHB = true
//                    msg = "Irregular heartbeat detected, Please try again"
//                    self.readingsLabel.text = "---"
//
//                    let alert = UIAlertController(title: "Alert!!!", message: msg, preferredStyle: .alert)
//
//                    alert.addAction(UIAlertAction(title: "OK", style: .default, handler: { [self] action in
//                        self.constantValue.ack = self.bleManagerReading.computeCheckSum(data: self.constantValue.ack)
//            //            print("checksum updated \(constantValue.ack)")
//                        self.writeOutGoingValue(data: self.constantValue.ack)
////                                readingsLabel.text = "---"
////                                            self.constantValue.is_irregularHB = false
//                    }))
//                    self.present(alert, animated: true)
//
//                    break
//
//                case 4:
//                    self.constantValue.is_errorReceived = true
//                    msg = "Cuff over pressurised, Please try again"
//                    self.readingsLabel.text = msg
//                    self.constantValue.ack = self.bleManagerReading.computeCheckSum(data: self.constantValue.ack)
//        //            print("checksum updated \(constantValue.ack)")
//                    self.writeOutGoingValue(data: self.constantValue.ack)
////                                        self.constantValue.is_errorReceived = true
//                    break
//
//                case 5:
//                    msg = "Low battery, Please charge the batteries"
//                    self.batteryLabel.backgroundColor = UIColor(hexString: "#FF0000")
//                    self.readingsLabel.text = "---"
//                    let alert = UIAlertController(title: "Alert!!!", message: msg, preferredStyle: .alert)
//
//                    alert.addAction(UIAlertAction(title: "OK", style: .default, handler: { [self] action in
//                        self.constantValue.cancelValue = self.bleManagerReading.computeCheckSum(data: self.constantValue.cancelValue)
//        //                print("checksum updated \(self.constantValue.ack)")
//                        writeOutGoingValue(data: self.constantValue.cancelValue)
////                                readingsLabel.text = "---"
////                                            self.constantValue.is = true
//                    }))
//                    self.present(alert, animated: true)
//
//                    break
//
//                case 6:
//                    self.constantValue.is_cuffReplaced = true
//                    Timer.invalidate()
//                    msg = "Please replace to new cuff!!!"
//                    self.readingsLabel.text = "---"
//
//                    let alert = UIAlertController(title: "Alert!!!", message: msg, preferredStyle: .alert)
//
//                    alert.addAction(UIAlertAction(title: "Reset", style: .default, handler: { [self] action in
//                        self.constantValue.resetValue = self.bleManagerReading.computeCheckSum(data: self.constantValue.resetValue)
//        //                print("checksum updated \(self.constantValue.ack)")
//                        self.writeOutGoingValue(data: self.constantValue.resetValue)
////                                            self.constantValue.is_cuffReplaced = false
////                                readingsLabel.text = "---"
//                    }))
//                    alert.addAction(UIAlertAction(title: "Cancel", style: .default, handler: { action in
//                        self.constantValue.noResetValue = self.bleManagerReading.computeCheckSum(data: self.constantValue.noResetValue)
//        //                print("checksum updated \(self.constantValue.ack)")
//                        self.writeOutGoingValue(data: self.constantValue.noResetValue)
////                                            self.constantValue.is_cuffReplaced = false
//                    }))
//                    self.present(alert, animated: true)
//                    //reset/ noreset
//                    break
//
//                case 7:
//                    self.constantValue.is_irregularHB = true
//                    Timer.invalidate()
//                    msg = "Heartbeat varied please retry again"
//                    self.readingsLabel.text = "---"
//                    let alert = UIAlertController(title: "Alert!!!", message: msg, preferredStyle: .alert)
//
//                    alert.addAction(UIAlertAction(title: "OK", style: .default, handler: { [self] action in
//                        self.constantValue.ack = self.bleManagerReading.computeCheckSum(data: self.constantValue.ack)
//            //            print("checksum updated \(constantValue.ack)")
//                        self.writeOutGoingValue(data: self.constantValue.ack)
////                                readingsLabel.text = "---"
////                                            self.constantValue.is_irregularHB = false
//                    }))
//                    self.present(alert, animated: true)
//
//                    break
//
//                case 8:
//                    self.constantValue.is_batterystatus = true
//                    Timer.invalidate()
//                    msg = "Battery level exceeded, Please change the battery"
//
//                    self.batteryLabel.backgroundColor = UIColor(hexString: "#a41e22")
//                    self.readingsLabel.text = "---"
//                    let alert = UIAlertController(title: "Alert!!!", message: "Battery level exceeded, Please change the battery", preferredStyle: .alert)
//
//                    alert.addAction(UIAlertAction(title: "OK", style: .default, handler: { [self] action in
//                        self.constantValue.cancelValue = self.bleManagerReading.computeCheckSum(data: self.constantValue.cancelValue)
//        //                print("checksum updated \(self.constantValue.ack)")
//                        writeOutGoingValue(data: self.constantValue.cancelValue)
////                                            self.constantValue.is_batterystatus = false
////                                readingsLabel.text = "---"
//                    }))
//                    self.present(alert, animated: true)
////
//                    break
//
//                case 11:
//                    self.constantValue.is_batterystatus = true
//                    Timer.invalidate()
//                    self.batteryLabel.backgroundColor = UIColor(hexString: "#FF0000")
////                                        self.constantValue.is_batterystatus = false
//                    break
//
//                default:
//                    break
//
//                }
//                break
//
//            case self.constantValue.ACK_COMMANDID:
//                print("ack: \(byteArray)")
//                Timer.invalidate()
//                self.constantValue.is_ackReceived = true
//                break
//
//            default:
//                break
//            }
//            self.secondsRemaining -= 1
////                                self.constantValue.is_ackReceived = false
//        } else {
//
//            if self.constantValue.is_battery_received == false {
//                Timer.invalidate()
//                    self.showToast(message: "Something went wrong, please try again", font: .systemFont(ofSize: 12))
//            }
//
//                else if self.constantValue.is_ackReceived == false {
//                    if self.constantValue.is_rawResultReceived == false {
//                        Timer.invalidate()
//                        self.showToast(message: "Please try again", font: .systemFont(ofSize: 12))
//                    }
//
//                    else if self.constantValue.is_rawResultReceived == false {
//                        Timer.invalidate()
//                        self.showToast(message: "Please try again", font: .systemFont(ofSize: 12))
//                    }
//
//                    else if self.constantValue.is_cuffReplaced == true {
//                        if self.constantValue.is_ackReceived == false {
//                            self.constantValue.cancelValue = self.bleManagerReading.computeCheckSum(data: self.constantValue.cancelValue)
//                    //        print("checksum updated \(constantValue.cancelValue)")
//                            self.writeOutGoingValue(data: self.constantValue.cancelValue)
//
//                        }
//                    }
//
//                    else if self.constantValue.is_errorReceived == false {
//                        Timer.invalidate()
//                        self.showToast(message: "Please try again", font: .systemFont(ofSize: 12))
//                    }
//
//                }
//
//
//        }
//    }


//switch byteArray[5] {
//case constantValue.DEVICE_COMMANDID:
//    print("device id: \(byteArray)")
//    constantValue.deviceId.remove(at: 0)
//    constantValue.deviceId.insert(byteArray[1], at: 0)
//    constantValue.deviceId.remove(at: 1)
//    constantValue.deviceId.insert(byteArray[2], at: 1)
//    constantValue.deviceId.remove(at: 2)
//    constantValue.deviceId.insert(byteArray[3], at: 2)
//    constantValue.deviceId.remove(at: 3)
//    constantValue.deviceId.insert(byteArray[4], at: 3)
//
//
////                    print("new device id \(constantValue.deviceId)")
//
//    constantValue.startValue = bleManagerReading.replaceDeviceVal(value: constantValue.startValue, value1: constantValue.deviceId)
////                print("new start value \(constantValue.startValue)")
//    constantValue.ack = bleManagerReading.replaceDeviceVal(value: constantValue.ack, value1: constantValue.deviceId)
//    constantValue.noAck = bleManagerReading.replaceDeviceVal(value: constantValue.noAck, value1: constantValue.deviceId)
//    constantValue.resetValue = bleManagerReading.replaceDeviceVal(value: constantValue.resetValue, value1: constantValue.deviceId)
//    constantValue.noResetValue = bleManagerReading.replaceDeviceVal(value: constantValue.noResetValue, value1: constantValue.deviceId)
//    constantValue.checkSumError = bleManagerReading.replaceDeviceVal(value: constantValue.checkSumError, value1: constantValue.deviceId)
//    break
//
//case constantValue.BATTERY_COMMANDID:
//
//    print("battery: \(byteArray)")
//    constantValue.is_battery_received = true
//    batteryVal = Int(byteArray[8])
////                    print("battery value: \(batteryVal)")
//    showBattery()
//
//    break
//
//case constantValue.RAW_COMMANDID:
////                        print("raw value: \(byteArray)")
////                    let readArray = byteArray.map { UInt16($0) }
//    constantValue.is_rawResultReceived = true
//    cuffval = newArray[8] * 256 + newArray[9]
//    print("cuff value \(cuffval)")
//    pulseVal = newArray[10] * 256 + newArray[11]
//    print("pulse value \(pulseVal)")
//
////                        if constantValue.is_ackReceived == true {
//        readingsLabel.text = "\(cuffval)" + " / " + "\(pulseVal)"
////                            constantValue.is_ackReceived = false
////                        }
//
//    break
//
//case constantValue.RESULT_COMMANDID:
//    print("result value: \(byteArray)")
//    constantValue.is_finalResultReceived = true
//    systolicVal = newArray[8] * 256 + newArray[9]
//    diastolicVal = newArray[10] * 256 + newArray[11]
//    heartRateVal = newArray[12]
//    map = newArray[13]
//
//    print("final result: \(systolicVal) / \(diastolicVal) / \(heartRateVal) / \(map)")
//
//    readingsLabel.text = "\(systolicVal) / \(diastolicVal) / \(heartRateVal)"
//
//    constantValue.ack = bleManagerReading.computeCheckSum(data: constantValue.ack)
////            print("checksum updated \(constantValue.ack)")
//    writeOutGoingValue(data: constantValue.ack)
//
//    break
//
//case constantValue.ERROR_COMMANDID:
//    print("error value: \(byteArray)")
//
//    let errorVal = byteArray[8]
//    var msg = ""
//    switch errorVal {
//    case 1:
//        constantValue.is_errorReceived = true
//        msg = "Cuff placement/fitment incorrect, Please change & try again"
//        readingsLabel.text = msg
//        constantValue.ack = bleManagerReading.computeCheckSum(data: constantValue.ack)
////            print("checksum updated \(constantValue.ack)")
//        writeOutGoingValue(data: constantValue.ack)
//        break
//
//    case 2:
//        constantValue.is_errorReceived = true
//        msg = "Hand movement detected, Please try again"
//        readingsLabel.text = msg
//        constantValue.ack = bleManagerReading.computeCheckSum(data: constantValue.ack)
////            print("checksum updated \(constantValue.ack)")
//        writeOutGoingValue(data: constantValue.ack)
//        break
//
//    case 3:
//        constantValue.is_irregularHB = true
//        msg = "Irregular heartbeat detected, Please try again"
//        readingsLabel.text = "---"
//
//        let alert = UIAlertController(title: "Alert!!!", message: msg, preferredStyle: .alert)
//
//        alert.addAction(UIAlertAction(title: "OK", style: .default, handler: { [self] action in
//            constantValue.ack = bleManagerReading.computeCheckSum(data: constantValue.ack)
////            print("checksum updated \(constantValue.ack)")
//            writeOutGoingValue(data: constantValue.ack)
////                                readingsLabel.text = "---"
//        }))
//        self.present(alert, animated: true)
//
//        break
//
//    case 4:
//        constantValue.is_errorReceived = true
//        msg = "Cuff over pressurised, Please try again"
//        readingsLabel.text = msg
//        constantValue.ack = bleManagerReading.computeCheckSum(data: constantValue.ack)
////            print("checksum updated \(constantValue.ack)")
//        writeOutGoingValue(data: constantValue.ack)
//        break
//
//    case 5:
//        msg = "Low battery, Please charge the batteries"
//        batteryLabel.backgroundColor = UIColor(hexString: "#FF0000")
//        readingsLabel.text = "---"
//        let alert = UIAlertController(title: "Alert!!!", message: msg, preferredStyle: .alert)
//
//        alert.addAction(UIAlertAction(title: "OK", style: .default, handler: { [self] action in
//            self.constantValue.cancelValue = self.bleManagerReading.computeCheckSum(data: self.constantValue.cancelValue)
////                print("checksum updated \(self.constantValue.ack)")
//            writeOutGoingValue(data: self.constantValue.cancelValue)
////                                readingsLabel.text = "---"
//        }))
//        self.present(alert, animated: true)
//
//        break
//
//    case 6:
//        constantValue.is_cuffReplaced = true
//        msg = "Please replace to new cuff!!!"
//        readingsLabel.text = "---"
//
//        let alert = UIAlertController(title: "Alert!!!", message: msg, preferredStyle: .alert)
//
//        alert.addAction(UIAlertAction(title: "Reset", style: .default, handler: { [self] action in
//            self.constantValue.resetValue = self.bleManagerReading.computeCheckSum(data: self.constantValue.resetValue)
////                print("checksum updated \(self.constantValue.ack)")
//            self.writeOutGoingValue(data: self.constantValue.resetValue)
////                                readingsLabel.text = "---"
//        }))
//        alert.addAction(UIAlertAction(title: "Cancel", style: .default, handler: { action in
//            self.constantValue.noResetValue = self.bleManagerReading.computeCheckSum(data: self.constantValue.noResetValue)
////                print("checksum updated \(self.constantValue.ack)")
//            self.writeOutGoingValue(data: self.constantValue.noResetValue)
//        }))
//        self.present(alert, animated: true)
//        //reset/ noreset
//        break
//
//    case 7:
//        constantValue.is_irregularHB = true
//        msg = "Heartbeat varied please retry again"
//        readingsLabel.text = "---"
//        let alert = UIAlertController(title: "Alert!!!", message: msg, preferredStyle: .alert)
//
//        alert.addAction(UIAlertAction(title: "OK", style: .default, handler: { [self] action in
//            constantValue.ack = bleManagerReading.computeCheckSum(data: constantValue.ack)
////            print("checksum updated \(constantValue.ack)")
//            writeOutGoingValue(data: constantValue.ack)
////                                readingsLabel.text = "---"
//        }))
//        self.present(alert, animated: true)
//
//        break
//
//    case 8:
//        constantValue.is_batterystatus = true
//        msg = "Battery level exceeded, Please change the battery"
//
//        batteryLabel.backgroundColor = UIColor(hexString: "#a41e22")
//        readingsLabel.text = "---"
//        let alert = UIAlertController(title: "Alert!!!", message: "Battery level exceeded, Please change the battery", preferredStyle: .alert)
//
//        alert.addAction(UIAlertAction(title: "OK", style: .default, handler: { [self] action in
//            self.constantValue.cancelValue = self.bleManagerReading.computeCheckSum(data: self.constantValue.cancelValue)
////                print("checksum updated \(self.constantValue.ack)")
//            writeOutGoingValue(data: self.constantValue.cancelValue)
////                                readingsLabel.text = "---"
//        }))
//        self.present(alert, animated: true)
////
//        break
//
//    case 11:
//        constantValue.is_batterystatus = true
//        batteryLabel.backgroundColor = UIColor(hexString: "#FF0000")
//        break
//
//    default:
//        break
//
//    }
//    break
//
//case constantValue.ACK_COMMANDID:
//    print("ack: \(byteArray)")
//    constantValue.is_ackReceived = true
//    break
//
//default:
//    break
//}