//
//  ConnectedDevicesViewController.swift
//  BP monitor app
//
//  Created by fueb on 27/06/22.
//  Copyright © 2022 fueb. All rights reserved.
//https://stackoverflow.com/questions/53526621/scanning-ble-peripheral-and-connecting-to-it

import UIKit
import CoreBluetooth
import CoreLocation

class ConnectedDevicesViewController: UIViewController , UITableViewDelegate, UITableViewDataSource, CLLocationManagerDelegate{
    
    var peripherialObj:[Peripheral] = []
    var cManager:CBCentralManager!
    let locationManager = CLLocationManager()
    var isConnected = false
    var index = 0
   
    var bleManager = BLEManager()
    var deviceName = ["1","2","3"]
    var deviceAddr = ["1","2","3"]

    var periperalData:[CBPeripheral] = []
    var peripheral1:CBPeripheral!
    
    @IBOutlet var scanTB: UITableView!
    override func viewDidLoad() {
        super.viewDidLoad()
        dataTransferClass = false
        self.navigationController?.navigationItem.hidesBackButton = true
        navigationItem.setHidesBackButton(true, animated: false)
        self.tabBarController?.tabBar.isHidden = false
        self.hidesBottomBarWhenPushed = false
  
        cManager = CBCentralManager(delegate: self, queue: nil, options: [CBCentralManagerOptionShowPowerAlertKey: true])
        
//        centralManager.delegate = self
        
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        self.navigationController?.navigationItem.hidesBackButton = true
        navigationItem.setHidesBackButton(true, animated: false)
        self.tabBarController?.tabBar.isHidden = false
        self.hidesBottomBarWhenPushed = false
        
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        var count = 0
        if (periperalData.count) > 0 {
            count = periperalData.count
        }
        return count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let deviceCell = tableView.dequeueReusableCell(withIdentifier: "scannedDevices")
        if periperalData[indexPath.row].name == nil {
            deviceCell?.textLabel?.text = "Unknown"
        }
        else {
            deviceCell?.textLabel?.text = periperalData[indexPath.row].name
        }
        return deviceCell!
    }
    
    @IBAction func scanBLE(_ sender: UIBarButtonItem) {
        startScanning()
//        scanTB.reloadData()
    }
    
    @IBAction func manualReading(_ sender: UIButton) {
        performSegue(withIdentifier: "toManualReadings", sender: self)
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        
      index = indexPath.row
        
        print("peripheral: \(periperalData[index])")
        peripheral1 = periperalData[index]
        peripheral1.delegate = self
        stopScanning()
        cManager.connect(peripheral1)
        
    }
    
//    @IBAction func backPressed(_ sender: UIBarButtonItem) {
//
//        performSegue(withIdentifier: "toHome", sender: self)
//    }
  
    // Passing the data to VC
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
//        if (segue.identifier == "toHome") {
//            self.tabBarController?.tabBar.isHidden = false
//        }
//        else
        if (segue.identifier == "toReadingVC") {
            let vc = segue.destination as! DataReadingViewController
            vc.periperalData = periperalData[index]
            vc.centralManager = cManager
        }
    }
}

// Mark :- delegate call to connect, discover and scan the BLE devices
extension ConnectedDevicesViewController:CBCentralManagerDelegate, CBPeripheralDelegate{
    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        switch central.state {
        case .unknown:
//            isPowerOn = false
            print("Bluetooth status is unknown")
            showToast(message: "Bluetooth is unknown", font: .systemFont(ofSize: 12))
//            msg = "Bluetooth is unknown"
            break
        case .resetting:
//            isPowerOn = false
            print("Bluetooth status is resetting")
            showToast(message: "Bluetooth is resetting", font: .systemFont(ofSize: 12))
//             msg = "Bluetooth is resetting"
            break
        case .unsupported:
//            isPowerOn = false
            print("Bluetooth status is unsupported")
            showToast(message: "Bluetooth is not supported", font: .systemFont(ofSize: 12))
//            msg = "Bluetooth is Unsupported"
            break
        case .unauthorized:
//            isPowerOn = false
            print("Bluetooth status is unauthorized")
            showToast(message: "Bluetooth is unauthorized", font: .systemFont(ofSize: 12))
//            msg = "Bluetooth is unauthorized"
            break
        case .poweredOff:
//            isPowerOn = false
            Constants.init().is_poweroff = true
            print("Central is not powered on")
//            msg = "Bluetooth is OFF"
            showToast(message: "Please turn on the bluetooth", font: .systemFont(ofSize: 12))
            cManager = CBCentralManager(delegate: self, queue: nil, options: [CBCentralManagerOptionShowPowerAlertKey: true])
            break
        case .poweredOn:
//            Bluetooth is ON
            break
        default:
            print("Other states")
//            msg = "Other states"
        }
    }
    
//    Scan bluetooth enabled device
    func startScanning() {
        if cManager.isScanning {
            showToast(message: "Scanning...", font: .systemFont(ofSize: 12))
        }
        else {
            cManager.scanForPeripherals(withServices:nil, options: nil)
        }
    }
    
//    Stop scannning
    func stopScanning() {
        cManager.stopScan()
    }
    
    func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: [String : Any], rssi RSSI: NSNumber) {
        peripherialObj.removeAll()
        var peripheralName:String!
        
        if let name = advertisementData[CBAdvertisementDataLocalNameKey] as? String {
            peripheralName = name
        }
        else {
            peripheralName = "Unknown"
        }
        
        let newPeripheral = Peripheral(id: peripherialObj.count, name: peripheralName, rssi: RSSI.intValue)
//        print("peripherial devices \(newPeripheral)")
        peripherialObj.append(newPeripheral)
        periperalData.append(peripheral)
        scanTB.reloadData()
    }
    
    func centralManager(_ central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral, error: Error?) {
        cManager.cancelPeripheralConnection(peripheral1)
        peripheral1 = nil
    }
    
    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        print("Connected!")
        peripheral.delegate = self
        peripheral.discoverServices(nil)
        
        //Method 1 : to navigate
        performSegue(withIdentifier: "toReadingVC", sender: self)
        
        //Method 2
//        let vc = storyboard?.instantiateViewController(withIdentifier: "readingsVC") as! DataReadingViewController
//        let nc = UINavigationController(rootViewController: vc)
//        //        vc.indexVal = index
//        vc.periperalData = periperalData[index]
//        vc.centralManager = cManager
//        self.present(nc, animated: false, completion: nil)
        //print("Discover services \(peripheral1.discoverServices(nil))")
    }
    
}
