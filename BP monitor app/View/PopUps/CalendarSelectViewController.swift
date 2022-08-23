//
//  CalendarSelectViewController.swift
//  BP monitor app
//
//  Created by fueb on 29/06/22.
//  Copyright © 2022 fueb. All rights reserved.
//

import UIKit
import FSCalendar

protocol enddateSelect {
    func selectedDate1(date:String)
}

class CalendarSelectViewController: UIViewController, FSCalendarDelegate, FSCalendarDataSource {
    
    @IBOutlet weak var yearLabel: UILabel!
    var delegate:enddateSelect?

    @IBOutlet weak var dateLabel: UILabel!
    
    @IBOutlet weak var calendarView1: FSCalendar!
    override func viewDidLoad() {
        super.viewDidLoad()

//        let calendar1 = FSCalendar(frame: CGRect(x: 0, y: 0, width: 350, height: 400))
        calendarView1.dataSource = self
        calendarView1.delegate = self
//        view.addSubview(calendar1)
//        self.calendarView1 = calendar1
        
        // Current date
        let mytime = Date()
        let format = DateFormatter()
        format.dateFormat = "E, MMM dd"
//        print(format.string(from: mytime))
        dateLabel.text = format.string(from: mytime)
        
        // Current date
        let mytime1 = Date()
        let format1 = DateFormatter()
        format1.dateFormat = "YYYY"
//        print(format1.string(from: mytime1))
        yearLabel.text = format1.string(from: mytime1)
        
        
    }
    
    func calendar(_ calendar: FSCalendar, didSelect date: Date, at monthPosition: FSCalendarMonthPosition) {
        let formatter = DateFormatter()
        formatter.dateFormat = "dd-MM-YYYY"
        let string = formatter.string(from: date)
//        print("string \(string)")
        delegate?.selectedDate1(date: string)
        
        let formatter1 = DateFormatter()
        formatter1.dateFormat = "YYYY"
//        print("year \(formatter1.string(from: date))")
        yearLabel.text = formatter1.string(from: date)
        
        let formatter2 = DateFormatter()
        formatter2.dateFormat = "E, MMM dd"
//        print("date \(formatter2.string(from: date))")
        dateLabel.text = formatter2.string(from: date)
    
        
//        dismiss(animated: true, completion: nil)
    }
    
    
    @IBAction func cancelBtn(_ sender: UIButton) {
        dismiss(animated: true, completion: nil)
    }
    
    @IBAction func okBtn(_ sender: UIButton) {
        dismiss(animated: true, completion: nil)
    }
    
}
