//
//  LocalNetworking.swift
//  BP monitor app
//
//  Created by fueb on 06/07/22.
//  Copyright © 2022 fueb. All rights reserved.
//

import UIKit
import CoreData

class LocalNetworking: NSObject {
    
    var isSuccess = false

    // Save to coredata
    func save(name:String,systolic:String,diastolic:String,heartRate:String,map:String,irregularHB:String) -> Bool {
        let appDelegate = UIApplication.shared.delegate as! AppDelegate
        
        let context = appDelegate.persistentContainer.viewContext
        let entity = NSEntityDescription.entity(forEntityName: "Reading", in: context)
        let newReadings = NSManagedObject(entity: entity!, insertInto: context)
        
        // Current date
        let mytime = Date()
        let format = DateFormatter()
        format.dateFormat = "dd-MM-yyyy"
        print(format.string(from: mytime))
        
        // Current time
        let mytime1 = Date()
        let format1 = DateFormatter()
        format1.timeStyle = .short
        format1.dateStyle = .none
        print(format1.string(from: mytime1))
        
        newReadings.setValue(name, forKey: "name")
        newReadings.setValue(systolic, forKey: "systolic")
        newReadings.setValue(diastolic, forKey: "diastolic")
        newReadings.setValue(heartRate, forKey: "heartRate")
        newReadings.setValue(map, forKey: "map")
        newReadings.setValue(irregularHB, forKey: "irregularHB")
        newReadings.setValue(format.string(from: mytime), forKey: "date")
        newReadings.setValue(format1.string(from: mytime1), forKey: "time")
        
        do {
            try context.save()
            return true
        } catch {
            print("Failed saving")
            return false
        }
    }
    
}
