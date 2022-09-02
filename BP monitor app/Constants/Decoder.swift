//
//  Decoder.swift
//  BP monitor app
//
//  Created by fueb on 28/06/22.
//  Copyright © 2022 fueb. All rights reserved.
//

import UIKit

class Decoder {
    
    init() {
        
    }
    
    // Status of the BP
    func changeStatus(systolic:Int, diastolic:Int) -> String {
        var msg:String?
        if((systolic < 80) || (diastolic < 60)) {
            msg = "Low Blood Pressure"
            
        }
        else if ((systolic <= 120) && (diastolic <= 80)){
            msg = "Normal Blood Pressure"
          
        }
        else if ((systolic <= 139) || (diastolic <= 89)) {
            msg = "High Normal Blood Pressure"
            
        }
        else if ((systolic <= 159) || (diastolic <= 99)) {
            msg = "Hypertension Stage 1"
            
        }
        else if ((systolic <= 179 ) || (diastolic <= 109)) {
            msg = "Hypertension Stage 2"
           
        }
        else {
            msg = "Hypertension Stage 3"
           
        }
        return msg!
    }
    
    
    
}
