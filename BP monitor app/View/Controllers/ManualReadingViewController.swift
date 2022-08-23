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
           self.showToast(message: "Please enter systolic value", font: .systemFont(ofSize: 12.0))
        }
        else if diastolicText.text == "" {
        self.showToast(message: "Please enter diastolic value", font: .systemFont(ofSize: 12.0))
        }
        else if heartRateText.text == "" {
            self.showToast(message: "Please enter heart rate value", font: .systemFont(ofSize: 12.0))
        }
        else {
            if ((Int(systolicText.text!)! < 30) || (Int(systolicText.text!)! > 200)) {
                let alert = UIAlertController(title: "Alert!", message: "Systolic range should be between 30 to 200,\n Please check the readings...", preferredStyle: .alert)
                alert.addAction(UIAlertAction(title: "OK", style: .default, handler: nil))
                self.present(alert, animated: true)
            }
            else if ((Int(diastolicText.text!)! < 40) || (Int(diastolicText.text!)! > 120)) {
                let alert = UIAlertController(title: "Alert!", message: "Diastolic range should be between 40 to 120,\n Please check the readings...", preferredStyle: .alert)
                alert.addAction(UIAlertAction(title: "OK", style: .default, handler: nil))
                self.present(alert, animated: true)
            }
            else {
                let mapText = bleManagerReading.calculateMap(systa: Int(systolicText.text!)!, diasta: Int(diastolicText.text!)!)
//                print("map  \(mapText)")
                let isSuccess = localDB.save(name: "Chaitra", systolic: systolicText.text!, diastolic: diastolicText.text!, heartRate: heartRateText.text!, map: String(describing: mapText))
                
                if isSuccess == true {
                    let alert = UIAlertController(title: "Success", message: "Saved successfully", preferredStyle: .alert)
                    alert.addAction(UIAlertAction(title: "OK", style: .default, handler: nil))
                    self.present(alert, animated: true)
                }
                else {
                    let alert = UIAlertController(title: "Unsuccess", message: "Failed to save", preferredStyle: .alert)
                    alert.addAction(UIAlertAction(title: "OK", style: .default, handler: nil))
                    self.present(alert, animated: true)
                }
            }
        }
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
