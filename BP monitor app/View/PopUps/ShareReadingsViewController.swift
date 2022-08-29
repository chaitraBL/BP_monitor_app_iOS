//
//  ShareReadingsViewController.swift
//  BP monitor app
//
//  Created by fueb on 29/06/22.
//  Copyright © 2022 fueb. All rights reserved.
//

import UIKit

class ShareReadingsViewController: UIViewController {
    
    
    @IBOutlet weak var systolicLabel: UILabel!
    @IBOutlet weak var diastolicLabel: UILabel!
    @IBOutlet weak var heartRateLabel: UILabel!
    @IBOutlet weak var statusLabel: UILabel!
    
    @IBOutlet weak var dateLabel: UILabel!
    
    var selectedSystolic:String?
    var selectedDiastolic:String?
    var selectedHeartRate:String?
    var selectedDate:String?
    var selectedStatus:String?
    
    override func viewDidLoad() {
        super.viewDidLoad()

        systolicLabel.text = selectedSystolic
        diastolicLabel.text = selectedDiastolic
        heartRateLabel.text = selectedHeartRate
        dateLabel.text = selectedDate
        
       changeStatus(systolic: Int(systolicLabel.text!)!, diastolic: Int(diastolicLabel.text!)!)
        
    }
    
    @IBAction func cancelBtn(_ sender: UIButton) {
        dismiss(animated: true, completion: nil)
    }
    
    // Share the readings through different apps
    @IBAction func shareBtn(_ sender: UIButton) {
        
        
//        statusLabel.text = selectedStatus
//        print("status \( statusLabel.text)")
        
        let text = "Blood Pressure Reading: \n\n\n" + "Systolic:           \(systolicLabel.text!)\nDiastolic:         \(diastolicLabel.text!)\nHeartRate:       \(heartRateLabel.text!)\nStatus:            \(statusLabel.text!)\nDate:              \(dateLabel.text!)"
        
        // set up activity view controller
        let textToShare = [ text ]
        let activityViewController = UIActivityViewController(activityItems: textToShare, applicationActivities: nil)
        activityViewController.popoverPresentationController?.sourceView = self.view // so that iPads won't crash
        
        // exclude some activity types from the list (optional)
//        activityViewController.excludedActivityTypes = [ UIActivity.ActivityType.airDrop, UIActivity.ActivityType.postToFacebook ]
        
        // present the view controller
        self.present(activityViewController, animated: true, completion: nil)
        
    }
    
    // Status of the BP
    func changeStatus(systolic:Int, diastolic:Int) {
        var msg:String?
        if((systolic < 80) || (diastolic < 60)) {
            msg = "Low Blood Pressure"
            statusLabel.text = msg
        }
        else if ((systolic <= 120) && (diastolic <= 80)){
            msg = "Normal Blood Pressure"
             statusLabel.text = msg
        }
        else if ((systolic <= 139) || (diastolic <= 89)) {
            msg = "High Normal Blood Pressure"
             statusLabel.text = msg
        }
        else if ((systolic <= 159) || (diastolic <= 99)) {
            msg = "Hypertension Stage 1"
             statusLabel.text = msg
        }
        else if ((systolic <= 179 ) || (diastolic <= 109)) {
            msg = "Hypertension Stage 2"
             statusLabel.text = msg
        }
        else {
            msg = "Hypertension Stage 3"
             statusLabel.text = msg
        }
        
    }
    
}
