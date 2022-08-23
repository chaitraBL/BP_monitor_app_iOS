
//
//  DataModel.swift
//  BP monitor app
//
//  Created by fueb on 29/06/22.
//  Copyright © 2022 fueb. All rights reserved.
//

import Foundation

struct filteredData
{
    var uid:String?
    var name:String?
    var systa:String?
    var diasta:String?
    var heartRate:String?
    var map:String?
    var date:String?
    var time:String?
    
    init(myname:String,mysysta:String,myDiasta:String,myRate:String,mymap:String,myDate:String,myTime:String){
        //        uid = uuid
        name = myname
        systa = mysysta
        diasta = myDiasta
        heartRate = myRate
        map = mymap
        date = myDate
        time = myTime
    }
    
    init(mydate:String) {
        date = mydate
    }
    
}

struct readingData
{
    var uid:String?
    var name:String?
    var systa:String?
    var diasta:String?
    var heartRate:String?
    var map:String?
    var date:String?
    var time:String?
    
    init(myname:String,mysysta:String,myDiasta:String,myRate:String,mymap:String,myDate:String,myTime:String){
        //        uid = uuid
        name = myname
        systa = mysysta
        diasta = myDiasta
        heartRate = myRate
        map = mymap
        date = myDate
        time = myTime
    }
    
    init(mydate:String) {
        date = mydate
    }
    
}
