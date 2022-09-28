//
//  ManualReadingViewController.swift
//  BP monitor app
//
//  Created by fueb on 28/06/22.
//  Copyright © 2022 fueb. All rights reserved.
//
// https://github.com/zekunyan/TTGSnackbar
//https://stackoverflow.com/questions/31540375/how-to-create-a-toast-message-in-swift

import UIKit
import CoreData


class ManualReadingViewController: UIViewController {

    var localDB = LocalNetworking()
    @IBOutlet weak var systolicText: UITextField!

    @IBOutlet weak var diastolicText: UITextField!
    
    @IBOutlet weak var heartRateText: UITextField!
    var bleManagerReading = BLEManager()
    
    override func viewDidLoad() {
        super.viewDidLoad()

        self.navigationController?.navigationItem.hidesBackButton = true
        self.tabBarController?.tabBar.isHidden = true
    }
    

    @IBAction func saveToDatabse(_ sender: UIButton) {
        if systolicText.text == "" {
            alert(title: "Alert!", msg: "Please enter systolic value")
        }
        else if diastolicText.text == "" {
            alert(title: "Alert!", msg: "Please enter diastolic value")
        }
        else if heartRateText.text == "" {
            alert(title: "Alert!", msg: "Please enter heart rate value")
        }
        else {
            if ((Int(systolicText.text!)! < 60) || (Int(systolicText.text!)! > 240)) {
                alert(title: "Alert!", msg: "Systolic range should be between 60 to 240,\n Please check the readings...")
                
            }
            else if ((Int(diastolicText.text!)! < 40) || (Int(diastolicText.text!)! > 130)) {
                alert(title: "Alert!", msg: "Diastolic range should be between 40 to 130,\n Please check the readings...")
               
            }
            else if ((Int(heartRateText.text!)! < 60) || (Int(heartRateText.text!)! > 140))
            {
                alert(title: "Alert!", msg: "Heartrate range should be between 60 to 140,\n Please check the readings...")
            }
            else {
                let mapText = bleManagerReading.calculateMap(systa: Int(systolicText.text!)!, diasta: Int(diastolicText.text!)!)
//                print("map  \(mapText)")
            
                let isSuccess = localDB.save(name: "Chaitra", systolic: systolicText.text!, diastolic: diastolicText.text!, heartRate: heartRateText.text!, map: String(describing: mapText), irregularHB: "0")
                
                if isSuccess == true {
                    alert(title: "Success", msg: "Saved successfully")
                }
                else {
                    alert(title: "Unsuccess", msg: "Failed to save")
                    
                }
            }
        }
    }
    
    @IBAction func backPressed(_ sender: UIBarButtonItem) {
//            performSegue(withIdentifier: "toScanDevices", sender: self)
        self.navigationController?.popViewController(animated: true)
    }
    
//    //    Navigating from one vc to another
//    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
//        if (segue.identifier == "toScanDevices") {
//            self.tabBarController?.tabBar.isHidden = false
//        }
//    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
       self.tabBarController?.tabBar.isHidden = false
   }
    
    func alert(title:String, msg:String) {
        let alert = UIAlertController(title: title, message: msg, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "OK", style: .default, handler: { action in
            self.systolicText.text = ""
            self.diastolicText.text = ""
            self.heartRateText.text = ""
        }))
        self.present(alert, animated: true)
    }
}

extension UIViewController {
    // Toast message interface
    func showToast(message : String, font: UIFont) {
//        self.view.frame.size.width/2 - 75
        let toastLabel = UILabel(frame: CGRect(x: 30, y: self.view.frame.size.height-100, width: 300, height: 50))
        toastLabel.backgroundColor = UIColor.black.withAlphaComponent(1) //0.6
        toastLabel.textColor = UIColor.white
        toastLabel.font = font
        toastLabel.textAlignment = .center;
        toastLabel.text = message
        toastLabel.alpha = 1.0
        toastLabel.layer.cornerRadius = 10;
        toastLabel.clipsToBounds  =  true
        self.view.addSubview(toastLabel)
        UIView.animate(withDuration: 4.0, delay: 0.5, options: .curveEaseOut, animations: {
            toastLabel.alpha = 0.0
        }, completion: {(isCompleted) in
            toastLabel.removeFromSuperview()
        })
    }
}
