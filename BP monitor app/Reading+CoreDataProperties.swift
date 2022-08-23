//
//  Reading+CoreDataProperties.swift
//  BP monitor app
//
//  Created by fueb on 25/07/22.
//  Copyright © 2022 fueb. All rights reserved.
//
//

import Foundation
import CoreData


extension Reading {

    @nonobjc public class func fetchRequest() -> NSFetchRequest<Reading> {
        return NSFetchRequest<Reading>(entityName: "Reading")
    }

    @NSManaged public var date: String?
    @NSManaged public var diastolic: String?
    @NSManaged public var heartRate: String?
    @NSManaged public var map: String?
    @NSManaged public var name: String?
    @NSManaged public var systolic: String?
    @NSManaged public var time: String?

}
