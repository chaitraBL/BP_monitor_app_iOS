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

    
    @IBOutlet var imgView1: UIImageView!
    @IBOutlet weak var imgView: UIImageView!
    var indexVal = 0
    @IBOutlet weak var statusLabel: UILabel!
    @IBOutlet weak var readingsLabel: UILabel!

    @IBOutlet var readingLab2: UILabel!
    @IBOutlet var readingLab1: UILabel!
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
    var decoderVal = Decoder()
    var batteryVal = 0
    var cuffval:UInt16 = 0
    var irregularHR:UInt16 = 0
    var pulseVal:UInt16 = 0
    var systolicVal:UInt16 = 0
    var diastolicVal:UInt16 = 0
    var heartRateVal:UInt16 = 0
    var map:UInt16 = 0
    var localDB = LocalNetworking()
    var bleManagerReading = BLEManager()
    var peripheralManager:CBPeripheralManager!
    var secondsRemaining = 0.7 // 20
    var secondsRemainingInReadings = 1000 //90
    @IBOutlet var activityView: UIView!
    let app = UIApplication.shared
    @IBOutlet var mapVal1: UILabel!
    @IBOutlet var alertLabel: UILabel!
    @IBOutlet var activityIndicator: UIActivityIndicatorView!
    
    var timerTest : Timer? = nil {
        willSet {
            timerTest?.invalidate()
        }
    }
    
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
        // Connection status
        connStatus = decodePeripheralState(peripheralState: periperalData.state, peripheral: periperalData)
        statusLabel.text = connStatus
        peripheralManager = CBPeripheralManager(delegate: self, queue: nil)
        peripheralInAppdelegate = periperalData
//        print ("perpheral data \(periperalData)")
        constantValue.is_finalResultReceived = false
        
        // Start btn and stop btn design layout
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
        constantValue.is_finalResultReceived = false
        
        // Background services
//        print ("perpheral data in view will appear \(periperalData)")
        let notificationCenter = NotificationCenter.default
            notificationCenter.addObserver(self, selector: #selector(appMovedToBackground), name: UIApplication.willResignActiveNotification, object: nil)
   
    }
  
    //Background Services
    @objc func appMovedToBackground() {
        print("App moved to background!")
        
        if constantValue.is_poweroff == false {
            //Timer
            Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { (Timer) in
                    if self.secondsRemaining > 0 {
                        print ("\(self.secondsRemaining) seconds")
                        self.secondsRemaining -= 0.1
                    } else if self.secondsRemaining == 0 {
                        print ("condition in zero sec \(String(describing: self.periperalData))")
                        Timer.invalidate()
                        self.secondsRemaining = 0.5
                        if self.periperalData == nil {
//                            self.centralManager.cancelPeripheralConnection(self.periperalData)
        //                    self.periperalData = nil

                            self.performSegue(withIdentifier: "disconnectPeripheral", sender: self)
                        }
                        else {
                                self.centralManager.cancelPeripheralConnection(self.periperalData)
                                //                    self.periperalData = nil

                                self.performSegue(withIdentifier: "disconnectPeripheral", sender: self)
                        }
                        
                    }
                }
        }
        else {
            print("power off")
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
        imgView1.image = img
    }
    
//    Disable bluetooth
    @IBAction func disableBluetooth(_ sender: UIBarButtonItem) {
        let alert = UIAlertController(title: "Alert!!!", message: "Are you sure, do you want to disconnect.", preferredStyle: .alert)

        alert.addAction(UIAlertAction(title: "OK", style: .default, handler: { [self] action in
            centralManager.cancelPeripheralConnection(periperalData)
            periperalData = nil
//            performSegue(withIdentifier: "disconnectPeripheral", sender: self)
            self.navigationController?.popViewController(animated: true)
        }))
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel, handler: nil))
        self.present(alert, animated: true)
    }
    
//    Navigating from one vc to another
override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
//    if (segue.identifier == "disconnectPeripheral") {
//        self.tabBarController?.tabBar.isHidden = false
//        self.navigationController?.popViewController(animated: true)
//    }
//    else
    if (segue.identifier == "toHome") {
        self.tabBarController?.tabBar.isHidden = false
    }
}
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
       self.tabBarController?.tabBar.isHidden = false
   }
  
//    Sending start command to receive readings
    @IBAction func startReading(_ sender: Any) {
        
        constantValue.is_startTapped = true
        constantValue.is_stopTapped = false
//        constantValue.is_battery_received = false
//        constantValue.is_irregularHB = false
//        constantValue.is_cuffReplaced = false
        constantValue.is_finalResultReceived = false
        constantValue.is_ackReceived = false
        constantValue.is_errorReceived = false
        constantValue.is_rawResultReceived = false
        constantValue.is_batterystatus = false
        constantValue.batteryPop = false
        
        systolicLabel.text = " "
        diastolicLab.text = " "
        heartRateLabel.text = " "
        mapLabel.text = " "
        readingLab1.text = "---"
        readingLab2.text = " "
        readingsLabel.text = "---"
        alertLabel.text = " "
        
        constantValue.startValue = bleManagerReading.computeCheckSum(data: constantValue.startValue)
//        print("checksum updated \(constantValue.startValue)")
        writeOutGoingValue(data: constantValue.startValue)
        
        Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { (Timer) in
                if self.secondsRemaining > 0 {
                    print ("\(self.secondsRemaining) seconds on start")
                    if self.constantValue.is_ackReceived == true {
                        Timer.invalidate()
                        self.secondsRemaining = 0.7
                    }
                    self.secondsRemaining -= 0.1
                } else if self.secondsRemaining == 0 {
//                    print("ack in start \(self.constantValue.is_ackReceived)")
                    if self.constantValue.is_ackReceived == false {
                        self.showToast(message: "Please start again", font: .systemFont(ofSize: 12))
                        self.secondsRemaining = 0.7
                        Timer.invalidate()
                    }
                }
            }
        
//        print("state \(periperalData)")
        // If disconnected from device end.
        if periperalData.state == .disconnected {
            alert1(msg: "Connection terminated!, please check the device connectivity")
        }
    }
    
//    Sending stop commands to stop receiving the readings
    @IBAction func stopReading(_ sender: UIButton) {
        constantValue.is_startTapped = false
        constantValue.is_stopTapped = true
        
//        constantValue.is_irregularHB = false
//        constantValue.is_cuffReplaced = false
//        constantValue.is_finalResultReceived = false
        constantValue.is_ackReceived = false
        constantValue.is_errorReceived = false
        constantValue.is_rawResultReceived = false
        constantValue.is_batterystatus = false
//        constantValue.cuffPop = false
//        constantValue.heartbeatPop = false
        
        readingLab2.text = ""
        readingLab1.text = "---"
        readingsLabel.text = "---"
        
        constantValue.cancelValue = bleManagerReading.computeCheckSum(data: constantValue.cancelValue)
//        print("checksum updated \(constantValue.cancelValue)")
        writeOutGoingValue(data: constantValue.cancelValue)
    
        Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { (Timer) in
                if self.secondsRemaining > 0 {
                    print ("\(self.secondsRemaining) seconds on stop")
                    if self.constantValue.is_ackReceived == true {
                        Timer.invalidate()
                        self.secondsRemaining = 0.7
                    }
                    self.secondsRemaining -= 0.1
                } else if self.secondsRemaining == 0 {
                    if self.constantValue.is_ackReceived == false {
                        self.constantValue.cancelValue = self.bleManagerReading.computeCheckSum(data: self.constantValue.cancelValue)
                //        print("checksum updated \(constantValue.cancelValue)")
                        self.writeOutGoingValue(data: self.constantValue.cancelValue)
                        self.secondsRemaining = 0.7
                    }
                }
            }
        
        // If disconnected from device end.
        if periperalData.state == .disconnected {
            alert1(msg: "Connection terminated!, please check the device connectivity")
        }
    }
    
//    Save the values to local db.
    func saveData(irregular : String) {
        print("to save \(constantValue.heartbeatPop)")
        if constantValue.heartbeatPop == false {
            
            let alert = UIAlertController(title: "Alert!!!", message: "Do you want to save the readings", preferredStyle: .alert)

            alert.addAction(UIAlertAction(title: "Yes", style: .default, handler: { [self] action in
                self.constantValue.heartbeatPop = true
                if ((systolicLabel.text == "-") || (diastolicLab.text == "-") || (heartRateLabel.text == "-") || (mapLabel.text == "-")) {
                    readingsLabel.text = "---"
                    readingLab1.text = "---"
                    readingLab2.text = ""
                    self.alert(title: "Alert!", msg: "Please check the reading value...", buttonName: "OK")
               }
           else if ((systolicLabel.text == " ") || (diastolicLab.text == " ") || (heartRateLabel.text == " ") || (mapLabel.text == " ")) {
               readingsLabel.text = "---"
               readingLab1.text = "---"
               readingLab2.text = ""
               self.alert(title: "Alert!", msg: "Please check the reading value...", buttonName: "OK")
           }
               else {
                   
                   if ((Int(systolicLabel.text!)! < 60) || (Int(systolicLabel.text!)! > 240)) {
                       readingsLabel.text = "---"
                       readingLab1.text = "---"
                       readingLab2.text = ""
                       self.alert(title: "Alert!", msg: "Systolic range should be between 60 to 240,\n Please check the readings...", buttonName: "OK")
                   }
                   else if ((Int(diastolicLab.text!)! < 40) || (Int(diastolicLab.text!)! > 130)) {
                       readingsLabel.text = "---"
                       readingLab1.text = "---"
                       readingLab2.text = ""
                       self.alert(title: "Alert!" , msg: "Diastolic range should be between 40 to 130,\n Please check the readings...", buttonName: "OK")
                   }
    
                   else {
                      
                       print("peripheral name = \(String(describing: periperalData.name))")
                       readingsLabel.text = "---"
                       readingLab1.text = "---"
                       readingLab2.text = ""
                       let isSuccess = localDB.save(name: periperalData.name!, systolic: systolicLabel.text!, diastolic: diastolicLab.text!, heartRate: heartRateLabel.text!, map: mapLabel.text!,irregularHB: irregular)
                       
                       if isSuccess == true {
                           self.alert(title: "Success", msg: "Saved successfully", buttonName: "OK")
                       }
                       else {
                           self.alert(title: "Unsuccess", msg: "Failed to save", buttonName: "OK")
                       }
                   }
               }
            }))
            alert.addAction(UIAlertAction(title: "No", style: .cancel, handler: { action in
                self.constantValue.heartbeatPop = true
                self.readingsLabel.text = "---"
                self.readingLab1.text = "---"
                self.readingLab2.text = ""
            }))
            self.present(alert, animated: true)
        }
    }
    
    // To display cuff replacement msg.
    func cuffReplace(irregular:String) {
        self.constantValue.heartbeatPop = false
        switch self.cuffval {
        case 0:
            print("not an cuff replacement error")
            saveData(irregular: irregular)
            break
        case 6:
            print("cuff replace detected")
//                                        msg = "Please replace to new cuff!!!"
            self.readingsLabel.text = "---"
            self.readingLab1.text = "---"
            self.readingLab2.text = ""

            if self.constantValue.cuffPop == false {
                let alert = UIAlertController(title: "Alert!!!", message: "Please replace to new cuff!!!", preferredStyle: .alert)

                alert.addAction(UIAlertAction(title: "Reset", style: .default, handler: { [self] action in
                    self.constantValue.cuffPop = true
                    self.constantValue.is_ackInCuff = true
                    self.constantValue.resetValue = self.bleManagerReading.computeCheckSum(data: self.constantValue.resetValue)
    //                print("checksum updated \(self.constantValue.ack)")
                    self.writeOutGoingValue(data: self.constantValue.resetValue)
                    
                    Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { (Timer) in
                            if self.secondsRemaining > 0 {
                                print ("\(self.secondsRemaining) seconds")
                                if self.constantValue.is_ackReceived == true {
                                    Timer.invalidate()
                                    self.saveData(irregular: irregular)
                                    self.secondsRemaining = 0.7
                                    
                                }
                                self.secondsRemaining -= 0.1
                            } else if self.secondsRemaining == 0 {
                                if self.constantValue.is_ackReceived == false {
                                    self.constantValue.resetValue = self.bleManagerReading.computeCheckSum(data: self.constantValue.resetValue)
                    //                print("checksum updated \(self.constantValue.ack)")
                                    self.writeOutGoingValue(data: self.constantValue.resetValue)
                                    self.secondsRemaining = 0.7
                                }
                            }
                        }
                }))
                alert.addAction(UIAlertAction(title: "Cancel", style: .cancel, handler: { action in
                    self.constantValue.cuffPop = true
                    self.constantValue.is_ackInCuff = true
                    self.constantValue.noResetValue = self.bleManagerReading.computeCheckSum(data: self.constantValue.noResetValue)
    //                print("checksum updated \(self.constantValue.ack)")
                    self.writeOutGoingValue(data: self.constantValue.noResetValue)
                    
                    Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { (Timer) in
                            if self.secondsRemaining > 0 {
                                print ("\(self.secondsRemaining) seconds")
                                if self.constantValue.is_ackReceived == true {
                                    Timer.invalidate()
                                    self.saveData(irregular: irregular)
                                    self.secondsRemaining = 0.7
                                    
                                }
                                self.secondsRemaining -= 0.1
                            } else if self.secondsRemaining == 0 {
                                if self.constantValue.is_ackReceived == false {
                                    self.constantValue.noResetValue = self.bleManagerReading.computeCheckSum(data: self.constantValue.noResetValue)
                    //                print("checksum updated \(self.constantValue.ack)")
                                    self.writeOutGoingValue(data: self.constantValue.noResetValue)
                                    self.secondsRemaining = 0.7
                                }
                            }
                        }
                }))
                self.present(alert, animated: true)
            }
            break
        default:
            print("Other cases")
            break
        }
    }
    
//    Display battery status
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
            
            let alert = UIAlertController(title: "Alert!!!", message: "Battery is low, Please charge and reconnect", preferredStyle: .alert)

            alert.addAction(UIAlertAction(title: "OK", style: .default, handler: { [self] action in
                self.constantValue.ack = self.bleManagerReading.computeCheckSum(data: self.constantValue.ack)
    //                print("checksum updated \(self.constantValue.ack)")
                writeOutGoingValue(data: self.constantValue.ack)
            }))
            self.present(alert, animated: true)
            
        }
        else{
            activityView.isHidden = true
            activityIndicator.stopAnimating()
            batteryLabel.backgroundColor = UIColor(hexString: "#a41e22")
//            showToast(message: "Battery level exceeded, Please change the battery", font: .systemFont(ofSize: 12))
            
//            alert2(msg:  "Battery level exceeded, Please change the battery")
            let alert = UIAlertController(title: "Alert!!!", message: "Battery level exceeded, Please change the battery", preferredStyle: .alert)

            alert.addAction(UIAlertAction(title: "OK", style: .default, handler: { [self] action in
                self.constantValue.ack = self.bleManagerReading.computeCheckSum(data: self.constantValue.ack)
    //                print("checksum updated \(self.constantValue.ack)")
                writeOutGoingValue(data: self.constantValue.ack)
            }))
            self.present(alert, animated: true)
        }
    }
    
//    Alert messages.
    func alert2(msg:String) {
        let alert = UIAlertController(title: "Alert!!!", message: msg, preferredStyle: .alert)

        alert.addAction(UIAlertAction(title: "OK", style: .default, handler: { [self] action in
            self.constantValue.cancelValue = self.bleManagerReading.computeCheckSum(data: self.constantValue.cancelValue)
                print("checksum updated")
            writeOutGoingValue(data: self.constantValue.cancelValue)
        }))
        self.present(alert, animated: true)
    }
    
    func alert(title:String, msg:String,buttonName:String) {
        let alert = UIAlertController(title: title, message: msg, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: buttonName, style: .default, handler: nil))
        present(alert, animated: true, completion: nil)
    }
    
    func alert1(msg:String) {
        let alert = UIAlertController(title: "Alert!!!", message: msg, preferredStyle: .alert)

        alert.addAction(UIAlertAction(title: "OK", style: .default, handler: { [self] action in
            centralManager.cancelPeripheralConnection(periperalData)
            periperalData = nil
            performSegue(withIdentifier: "disconnectPeripheral", sender: self)
        }))
        self.present(alert, animated: true)
    }
}

extension DataReadingViewController: CBPeripheralDelegate {
  
//    Checking the peripheral state
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
       
//    Service discovery
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
        }
        
//    Characteristics discovery
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
        }
        
//    Updating notification state
        func peripheral(_ peripheral: CBPeripheral, didUpdateNotificationStateFor characteristic: CBCharacteristic, error: Error?) {
            if (error != nil) {
                print("error reading characteristics \(String(describing: error?.localizedDescription))")
            }
            if (characteristic.isNotifying) {
                print("notification began \(characteristic)")
            }
        }
        
//    Receiving values
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
                                Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { (Timer) in
                                        if self.secondsRemaining > 0 {
                                            print ("\(self.secondsRemaining) seconds")
                                            self.secondsRemaining -= 0.1
                                        } else if self.secondsRemaining == 0 {
//                                            if self.constantValue.is_battery_received == false {
                                                self.alert(title: "alert!!!", msg: "Something went wrong, Please reconnect again", buttonName: "Ok")
                                                Timer.invalidate()
                                            self.secondsRemaining = 0.7
//                                            }
                                            
                                        }
                                    }
                            }
                }
            }
        }
        
//    To receive and display data
        func broadcastUpdate(characteristic:CBCharacteristic)  {
//            self.secondsRemainingInReadings = 90
            guard let characteristicData = characteristic.value else {return}
            print("character data: \(characteristic)")
            let byteArray = [UInt8](characteristicData)
            
//            Timer to check device id is received or not
            var secondsRemainingForDeviceId = 3
            Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { (Timer) in
                    if secondsRemainingForDeviceId > 0 {
                        print ("\(secondsRemainingForDeviceId) seconds")
                        if self.constantValue.is_deviceReceived == true {
                            print("device id received")
                            Timer.invalidate()
                            secondsRemainingForDeviceId = 3
                        }
                        secondsRemainingForDeviceId -= 1
                    } else if secondsRemainingForDeviceId == 0 {
                        if self.constantValue.is_deviceReceived == false {
                            self.alert(title: "alert!!!", msg: "Something went wrong, Please check the device connection", buttonName: "Ok")
                            Timer.invalidate()
                            secondsRemainingForDeviceId = 3
                        }
                    }
                }
            
//            print("byte array \(byteArray) ")
//            print("hex array: \(byteArray.bytesToHex(spacing: " "))")
            if byteArray[0] == 123 || byteArray[0] == 91 || byteArray[0] == 40 {
                
                print("command id \(byteArray[5])")
                
                let newArray = byteArray.map { UInt16($0) }

    //            print("new array \(newArray)")
                let verified = bleManagerReading.checkSumValidation(data: newArray, characteristics: characteristic)
                print("checksum verified \(verified)")

                if verified == true {
                    
                    Timer.scheduledTimer(withTimeInterval: 0.09, repeats: true) { (timer) in //0.1 /0.09, false
                            if self.secondsRemainingInReadings > 0 {
                                
                                
                                print ("\(self.secondsRemainingInReadings) seconds")
                                switch byteArray[5] {
//                                    Device Id
                                case self.constantValue.DEVICE_COMMANDID:
                                    print("device id: \(byteArray)")
                                    self.constantValue.is_deviceReceived = true
                    
                                    timer.invalidate()
//                                    self.secondsRemainingInReadings = 90
                                    
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
                                    
                                    Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { (Timer) in
                                            if secondsRemainingForDeviceId > 0 {
                                                print ("\(self.secondsRemaining) seconds")
                                                if self.constantValue.is_battery_received == true {
                                                    Timer.invalidate()
                                                    secondsRemainingForDeviceId = 3
                                                }
                                                secondsRemainingForDeviceId -= 1
                                            } else if secondsRemainingForDeviceId == 0 {
                                                if self.constantValue.is_battery_received == false {
                                                    self.alert(title: "alert!!!", msg: "Something went wrong, Please reconnect again", buttonName: "Ok")
                                                    Timer.invalidate()
                                                    secondsRemainingForDeviceId = 3
                                                }
                                            }
                                        }
                                    break
                                    
//                    Battery status
                                case self.constantValue.BATTERY_COMMANDID:
                                    timer.invalidate()
                                    self.secondsRemainingInReadings = 1000
                                    print("battery: \(byteArray)")
                                    self.constantValue.is_battery_received = true
                                    self.batteryVal = Int(byteArray[8])
                    //                    print("battery value: \(batteryVal)")
                                    self.showBattery()
                                    break
                    
//                                    Raw readings
                                case self.constantValue.RAW_COMMANDID:
                                    timer.invalidate()
                                    self.secondsRemainingInReadings = 1000
                                    self.constantValue.is_rawResultReceived = true
                                    self.cuffval = newArray[8] * 256 + newArray[9]
                                    print("cuff value \(self.cuffval)")
                                    self.pulseVal = newArray[10] * 256 + newArray[11]
                                    print("pulse value \(self.pulseVal)")
                    
                                    self.readingLab1.text = "\(self.cuffval)" + " / " + "\(self.pulseVal)"
                                    break
                    
//                                    Final results
                                case self.constantValue.RESULT_COMMANDID:
//                                    print("result value: \(byteArray)")
                                    
                                    timer.invalidate()
                                    self.secondsRemainingInReadings = 1000
                                    self.constantValue.is_finalResultReceived = true
                                    
                                    self.systolicVal = newArray[8] * 256 + newArray[9]
                                    self.diastolicVal = newArray[10] * 256 + newArray[11]
                                    self.heartRateVal = newArray[12]
                                    self.map = newArray[13]
                                    self.irregularHR = newArray[14]
                                    self.cuffval = newArray[15]
                    
                                    print("final result: \(self.systolicVal) / \(self.diastolicVal) / \(self.heartRateVal) / \(self.map)")
                                    
                                    self.constantValue.cancelValue = self.bleManagerReading.computeCheckSum(data: self.constantValue.cancelValue)
                        //            print("checksum updated \(constantValue.ack)")
                                    self.writeOutGoingValue(data: self.constantValue.cancelValue)
                                    
                                    self.stopBtn.isHidden = true
                                    self.startbtn.isHidden = false
                                    self.stopBtn.isEnabled = false
                                    self.startbtn.isEnabled = true
                                    self.constantValue.cuffPop = false
                                    self.constantValue.heartbeatPop = false
                                    
                                    if (self.systolicVal < 60) || (self.systolicVal > 230) {
                                        self.readingLab1.text = "---"
                                        self.readingLab2.text = ""
                                        self.readingsLabel.text = "---"
                                        self.alert(title: "Alert!!!", msg: "Systolic value is out of range, Please try again", buttonName: "Ok")
                                    }
                                    else if (self.diastolicVal < 40) || (self.diastolicVal > 130) {
                                        self.readingLab1.text = "---"
                                        self.readingLab2.text = ""
                                        self.readingsLabel.text = "---"
                                        self.alert(title: "Alert!!!", msg: "Diastolic value is out of range, Please try again", buttonName: "Ok")
                                    }
                                    else if (self.heartRateVal < 60) || (self.heartRateVal > 120) {
                                        self.readingLab1.text = "---"
                                        self.readingLab2.text = ""
                                        
                                        let alert = UIAlertController(title: "Alert!!!", message: "Heart rate value is out of range, Please try again", preferredStyle: .alert)

                                        alert.addAction(UIAlertAction(title: "OK", style: .default, handler: { [self] action in
                                           
                                            self.readingLab1.text = "\(self.systolicVal) / \(self.diastolicVal) mmHg"
                                            self.readingLab2.text = "\(self.heartRateVal) bpm"
                                            
                                            self.systolicLabel.text = "\(self.systolicVal)"
                                            self.diastolicLab.text = "\(self.diastolicVal)"
                                            self.heartRateLabel.text = "\(self.heartRateVal)"
                                            self.mapVal1.text = "\(self.map)"
                                            let msg = self.decoderVal.changeStatus(systolic: Int(self.systolicVal), diastolic: Int(self.diastolicVal))
                                            self.mapLabel.text = msg
                                            
//                                            If irregular heart rate found.
                                            
                                            print("irregular hb \(self.irregularHR)")
                                            
                                            switch self.irregularHR {
                                            case 0:
                                                print("Not an irregular HB")
                                                self.cuffReplace(irregular: "0")
                                                break
                                            case 3:
                                                print("irregular hb detected")
                                                self.readingsLabel.text = "Irregular heart rate while measurement"
                                                self.alertLabel.text = "Irregular heart rate while measurement"
                                                self.cuffReplace(irregular: self.alertLabel.text!)
                                                break
                                            case 7:
                                                print("hb varied detected")
                                                self.readingsLabel.text = "Heart rate varied"
                                                self.alertLabel.text = "Heart rate varied"
                                                self.cuffReplace(irregular: self.alertLabel.text!)
                                                break
                                            default:
                                                print("Other cases")
                                                break
                                            }
                                              
                                        }))
                                        self.present(alert, animated: true)
                                        
                                    }
                                    else {
                                        self.readingLab1.text = "\(self.systolicVal) / \(self.diastolicVal) mmHg"
                                        self.readingLab2.text = "\(self.heartRateVal) bpm"
                                        
                                        self.systolicLabel.text = "\(self.systolicVal)"
                                        self.diastolicLab.text = "\(self.diastolicVal)"
                                        self.heartRateLabel.text = "\(self.heartRateVal)"
                                        self.mapVal1.text = "\(self.map)"
                                        let msg = self.decoderVal.changeStatus(systolic: Int(self.systolicVal), diastolic: Int(self.diastolicVal))
                                        self.mapLabel.text = msg
                                        
                                        //  If irregular heart rate found.
                                        switch self.irregularHR {
                                        case 0:
                                            print("Not an irregular HB")
                                            
                                            self.cuffReplace(irregular: "0")
                                            break
                                        case 3:
                                            self.readingsLabel.text = "Irregular heart rate while measurement"
                                            self.alertLabel.text = "Irregular heart rate while measurement"
                                            self.cuffReplace(irregular: self.alertLabel.text!)
                                            break
                                        case 7:
                                            self.readingsLabel.text = "Heart rate varied"
                                            self.alertLabel.text = "Heart rate varied"
                                            self.cuffReplace(irregular: self.alertLabel.text!)
                                            break
                                        default:
                                            print("Other cases")
                                            break
                                        }
                                    }
                                    self.constantValue.is_finalResultReceived = false
                                    break
                    
//                              Error msgs
                                case self.constantValue.ERROR_COMMANDID:
                                    print("error value: \(byteArray)")
                                    
                                    let errorVal = byteArray[8]
                                    var msg = ""
                                    
                                    switch errorVal {
                                        // Cuff placement error
                                    case 1:
                                        timer.invalidate()
                                        self.secondsRemainingInReadings = 1000
                                        self.constantValue.is_errorReceived = true
                                        msg = "Cuff placement/fitment incorrect"
                                        self.readingLab1.text = "---"
                                        self.readingLab2.text = ""
                                        self.readingsLabel.text = msg
                                        self.alertLabel.text = msg
                                        self.constantValue.cancelValue = self.bleManagerReading.computeCheckSum(data: self.constantValue.cancelValue)
                            //            print("checksum updated \(constantValue.ack)")
                                        self.writeOutGoingValue(data: self.constantValue.cancelValue)
                                        self.stopBtn.isHidden = true
                                        self.startbtn.isHidden = false
                                        self.stopBtn.isEnabled = false
                                        self.startbtn.isEnabled = true
                                        break
                    
                                        // Hand movement error
                                    case 2:
                                        timer.invalidate()
                                        self.secondsRemainingInReadings = 1000
                                        self.constantValue.is_errorReceived = true
                                        msg = "Hand movement detected"
                                        self.readingLab1.text = "---"
                                        self.readingLab2.text = ""
                                        self.readingsLabel.text = msg
                                        self.alertLabel.text = msg
                                        self.constantValue.cancelValue = self.bleManagerReading.computeCheckSum(data: self.constantValue.cancelValue)
                            //            print("checksum updated \(constantValue.ack)")
                                        self.writeOutGoingValue(data: self.constantValue.cancelValue)
                                        self.stopBtn.isHidden = true
                                        self.startbtn.isHidden = false
                                        self.stopBtn.isEnabled = false
                                        self.startbtn.isEnabled = true
                                        break
                    
                                        // cuff pressurised error
                                    case 4:
                                        timer.invalidate()
                                        self.secondsRemainingInReadings = 1000
                                        self.constantValue.is_errorReceived = true
                                        msg = "Cuff over pressurised"
                                        self.readingLab1.text = "---"
                                        self.readingLab2.text = ""
                                        self.readingsLabel.text = msg
                                        self.alertLabel.text = msg
                                        self.constantValue.cancelValue = self.bleManagerReading.computeCheckSum(data: self.constantValue.cancelValue)
                            //            print("checksum updated \(constantValue.ack)")
                                        self.writeOutGoingValue(data: self.constantValue.cancelValue)
                                        self.stopBtn.isHidden = true
                                        self.startbtn.isHidden = false
                                        self.stopBtn.isEnabled = false
                                        self.startbtn.isEnabled = true
                                        break
                    
                                        // battery status error
                                    case 5:
                                        timer.invalidate()
                                        self.secondsRemainingInReadings = 90
//                                        self.constantValue.batteryPop = false
                                        msg = "Low battery, Please charge the batteries"
                                        self.batteryLabel.backgroundColor = UIColor(hexString: "#FF0000")
                                        self.readingsLabel.text = "---"
                                        self.readingLab1.text = "---"
                                        self.readingLab2.text = ""
                                        
                                        if self.constantValue.batteryPop == false {
                                            let alert = UIAlertController(title: "Alert!!!", message: msg, preferredStyle: .alert)
                        
                                            alert.addAction(UIAlertAction(title: "OK", style: .default, handler: { [self] action in
                                                self.constantValue.batteryPop = true
                                                self.constantValue.is_ackInBattery = true
                                                self.constantValue.cancelValue = self.bleManagerReading.computeCheckSum(data: self.constantValue.cancelValue)
                                //                print("checksum updated \(self.constantValue.ack)")
                                                writeOutGoingValue(data: self.constantValue.cancelValue)
                                                self.stopBtn.isHidden = true
                                                self.startbtn.isHidden = false
                                                self.stopBtn.isEnabled = false
                                                self.startbtn.isEnabled = true
    
                                            }))
                                            self.present(alert, animated: true)
                                            self.constantValue.batteryPop = true
                                        }
                                        break
                    
                                    case 8:
                                        self.constantValue.is_batterystatus = true
//                                        self.constantValue.batteryPop = false
                                        timer.invalidate()
                                        self.secondsRemainingInReadings = 1000
                                        msg = "Battery level exceeded, Please change the battery"
                    
                                        self.batteryLabel.backgroundColor = UIColor(hexString: "#a41e22")
                                        self.readingsLabel.text = "---"
                                        self.readingLab1.text = "---"
                                        self.readingLab2.text = ""
                                        print("battery pop \(self.constantValue.batteryPop)")
                                        if self.constantValue.batteryPop == false {
                                            let alert = UIAlertController(title: "Alert!!!", message: "Battery level exceeded, Please change the battery", preferredStyle: .alert)
                        
                                            alert.addAction(UIAlertAction(title: "OK", style: .default, handler: { [self] action in
                                                self.constantValue.batteryPop = true
                                                self.constantValue.is_ackInBattery = true
                                                self.constantValue.cancelValue = self.bleManagerReading.computeCheckSum(data: self.constantValue.cancelValue)
                            
                                                writeOutGoingValue(data: self.constantValue.cancelValue)
                                                self.stopBtn.isHidden = true
                                                self.startbtn.isHidden = false
                                                self.stopBtn.isEnabled = false
                                                self.startbtn.isEnabled = true
    
                                            }))
                                            self.present(alert, animated: true)
                                            self.constantValue.batteryPop = true
                                        }
                                        break
                    
                                        // not used
                                    case 11:
                                        self.constantValue.is_batterystatus = true
                                        timer.invalidate()
                                        self.secondsRemainingInReadings = 1000
                                        self.batteryLabel.backgroundColor = UIColor(hexString: "#FF0000")
                                        self.stopBtn.isHidden = true
                                        self.startbtn.isHidden = false
                                        self.stopBtn.isEnabled = false
                                        self.startbtn.isEnabled = true
                                        break
                    
                                    default:
                                        break
                    
                                    }
                                    self.constantValue.is_errorReceived = false
                                    break
                    
//                                    Acknowledgement
                                case self.constantValue.ACK_COMMANDID:
                                    print("ack: \(byteArray)")
                                    
                                    timer.invalidate()
//                                    self.secondsRemainingInReadings = 90
                                    self.constantValue.is_ackReceived = true
                                    
                                    // Ack received for stop command
                                    if self.constantValue.is_stopTapped == true {
                                        self.constantValue.is_ackReceived = true
                                        self.stopBtn.isHidden = true
                                        self.startbtn.isHidden = false
                                        self.stopBtn.isEnabled = false
                                        self.startbtn.isEnabled = true
                                        self.readingsLabel.text = "---"
                                        self.readingLab1.text = "---"
                                        self.readingLab2.text = ""
                                        self.constantValue.is_stopTapped = false
                                    }
                                    
                                    // Ack received for start command
                                    else if self.constantValue.is_startTapped == true {
                                        self.constantValue.is_ackReceived = true
                                        self.stopBtn.isHidden = false
                                        self.startbtn.isHidden = true
                                        self.stopBtn.isEnabled = true
                                        self.startbtn.isEnabled = false
                                        self.secondsRemainingInReadings = 1000
                                        self.constantValue.is_startTapped = false
                                    }
                                    
                                    // Ack received for cuff replacement
                                    else if self.constantValue.is_ackInCuff == true {
                                        self.constantValue.is_ackReceived = true
                                        self.readingsLabel.text = "---"
                                        self.readingLab1.text = "---"
                                        self.readingLab2.text = ""
                                        self.constantValue.is_ackInCuff = false
                                    }
                                    
                                    else if self.constantValue.is_ackInBattery == true {
                                        self.constantValue.is_ackReceived = true
                                        self.readingsLabel.text = "---"
                                        self.readingLab1.text = "---"
                                        self.readingLab2.text = ""
                                        self.constantValue.is_ackInBattery = false
                                    }
                                    break
                    
                                default:
                                    break
                                }
                                self.secondsRemainingInReadings -= 1
                                
                            } else if self.secondsRemainingInReadings == 0 {
//                                If final results or error msg not received.
                                if self.constantValue.is_finalResultReceived == false || self.constantValue.is_errorReceived == false {
//                                    print
                                    timer.invalidate()
                                    self.secondsRemainingInReadings = 1000
                                    self.alert2(msg: "Please start again")
                                    self.stopBtn.isHidden = true
                                    self.startbtn.isHidden = false
                                    self.stopBtn.isEnabled = false
                                    self.startbtn.isEnabled = true
                                }
                               }
                            }
                        }
                }
            //Checksum error
                else {
                    self.constantValue.checkSumError = self.bleManagerReading.computeCheckSum(data: self.constantValue.checkSumError)
    //                print("checksum updated \(self.constantValue.checkSumError)")
                    writeOutGoingValue(data: self.constantValue.checkSumError)
                }
//            }
        }
        
//    Write values to the device
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
            constantValue.is_poweroff = true
            
            if periperalData != nil {
                    
                    centralManager.cancelPeripheralConnection(periperalData)
                    periperalData = nil
                    performSegue(withIdentifier: "toHome", sender: self)
            }
            
            else {
                dismiss(animated: true)
            }
            return
            
        case .resetting:
            print("Resetting")
            constantValue.is_poweroff = true
            if periperalData != nil {
                    
                    centralManager.cancelPeripheralConnection(periperalData)
                    periperalData = nil
                    performSegue(withIdentifier: "toHome", sender: self)
            }
            
            else {
                dismiss(animated: true)
            }
            
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
