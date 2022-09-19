//
//  ChartMarker.swift
//  BP monitor app
//
//  Created by fueb on 06/09/22.
//  Copyright © 2022 fueb. All rights reserved.
//

import Foundation
import Charts

class ChartMarker:  MarkerView{
    var text = ""
    var text1 = ""
    
    override func refreshContent(entry: ChartDataEntry, highlight: Highlight) {
           super.refreshContent(entry: entry, highlight: highlight)
        
//        guard let e = CandleChartDataSet.self as? CandleChartDataEntry else { return }
        guard let e = entry as? CandleChartDataEntry else {return}
//        print(e.high)

        text = "Systolic " + String(e.high) + "\nDiastolic " + String(e.low)
//        text1 = String(e.x)
       }

       override func draw(context: CGContext, point: CGPoint) {
           super.draw(context: context, point: point)

           var drawAttributes = [NSAttributedString.Key : Any]()
           drawAttributes[.font] = UIFont.systemFont(ofSize: 15)
           drawAttributes[.foregroundColor] = UIColor.white
           drawAttributes[.backgroundColor] = UIColor.init(hexString: "#151B54")

           self.bounds.size = (" \(text) " as NSString).size(withAttributes: drawAttributes)
           self.offset = CGPoint(x: 0, y: -self.bounds.size.height - 2)

           let offset = self.offsetForDrawing(atPoint: point)

           drawText(text: " \(text) " as NSString, rect: CGRect(origin: CGPoint(x: point.x + offset.x, y: point.y + offset.y), size: self.bounds.size), withAttributes: drawAttributes)
       }

    func drawText(text: NSString, rect: CGRect, withAttributes attributes: [NSAttributedString.Key : Any]? = nil) {
           let size = text.size(withAttributes: attributes)
           let centeredRect = CGRect(x: rect.origin.x + (rect.size.width - size.width) / 2.0, y: rect.origin.y + (rect.size.height - size.height) / 2.0, width: size.width, height: size.height)
           text.draw(in: centeredRect, withAttributes: attributes)
       }
    
}
