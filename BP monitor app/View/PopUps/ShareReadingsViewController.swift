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
    
    @IBOutlet var irregular: UILabel!
    @IBOutlet weak var dateLabel: UILabel!
    
    var selectedSystolic:String?
    var selectedDiastolic:String?
    var selectedHeartRate:String?
    var selectedDate:String?
    var selectedStatus:String?
    var selectedIrregular:String?
    var irregularHB:String?
    var decoderVal = Decoder()
    
    override func viewDidLoad() {
        super.viewDidLoad()

        systolicLabel.text = selectedSystolic
        diastolicLabel.text = selectedDiastolic
        heartRateLabel.text = selectedHeartRate
        dateLabel.text = selectedDate
        
        if selectedIrregular == "0" {
            irregular.text = " "
        }
        else {
            irregular.text = selectedIrregular
//            irregularHB = "(" + selectedIrregular! + ")"
        }
        
        let msg = decoderVal.changeStatus(systolic: Int(systolicLabel.text!)!, diastolic: Int(diastolicLabel.text!)!)
        statusLabel.text = msg
        
    }
    
    @IBAction func cancelBtn(_ sender: UIButton) {
        dismiss(animated: true, completion: nil)
    }
    
    // Share the readings through different apps
    @IBAction func shareBtn(_ sender: UIButton) {
        
//        statusLabel.text = selectedStatus
        print("status \( statusLabel.text)")
        
        let text = "Blood Pressure Reading: \n\n\n" + "Systolic:           \(systolicLabel.text!)\nDiastolic:         \(diastolicLabel.text!)\nHeartRate:       \(heartRateLabel.text!) (\(irregular.text!))\nStatus:            \(statusLabel.text!)\nDate:              \(dateLabel.text!)"
        
        // set up activity view controller
        let textToShare = [ text ]
        let activityViewController = UIActivityViewController(activityItems: textToShare, applicationActivities: nil)
        activityViewController.popoverPresentationController?.sourceView = self.view // so that iPads won't crash
        
        // exclude some activity types from the list (optional)
        activityViewController.excludedActivityTypes = [ UIActivity.ActivityType.airDrop, UIActivity.ActivityType.postToFacebook ]
        
        // present the view controller
        self.present(activityViewController, animated: true, completion: nil)
        
    }
    
}
