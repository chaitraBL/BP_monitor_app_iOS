//
//  Readings+CoreDataProperties.swift
//  
//
//  Created by fueb on 28/06/22.
//
//

import Foundation
import CoreData


extension Readings {

    @nonobjc public class func fetchRequest() -> NSFetchRequest<Readings> {
        return NSFetchRequest<Readings>(entityName: "Readings")
    }

    @NSManaged public var name: String?
    @NSManaged public var systolic: String?
    @NSManaged public var diastolic: String?
    @NSManaged public var heartRate: String?
    @NSManaged public var map: String?

}
