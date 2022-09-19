//
//  LogsViewController.swift
//  BP monitor app
//
//  Created by fueb on 28/06/22.
//  Copyright © 2022 fueb. All rights reserved.
//https://www.idownloadblog.com/2015/12/24/how-to-create-a-free-apple-developer-account-xcode/ - developer account


import UIKit
import CoreData

class LogsViewController: UIViewController, UITableViewDelegate, UITableViewDataSource, enddateSelect, UIPopoverPresentationControllerDelegate, startdateSelect {
   
    
    @IBOutlet weak var errorLabel: UILabel!
    var logObj:[Reading] = []
    
    
    @IBOutlet weak var startDate: UILabel!
    var selectedDateArray:[String] = []
    var filteredArray:[filteredData] = []
    var localReadingList:[readingData] = []
    
    @IBOutlet weak var endDate: UILabel!
    @IBOutlet weak var logTableView: UITableView!
    
    var selectedStartDate:String?
    var selectedEndDate:String?
    
    // Protocol stubs(methods)
    func selectedDate1(date: String) {
//        print("end date \(date)")
        endDate.text = date
        endDate.textColor = .black
        selectedEndDate = endDate.text
    }
    
    // Protocol stubs(methods)
    func selectedDate(date: String) {
//        print("start date \(date)")
        startDate.text = date
        startDate.textColor = .black
        selectedStartDate = startDate.text
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()

        UITabBarItem.appearance().setTitleTextAttributes([NSAttributedString.Key.foregroundColor:UIColor(hexString: "#162760")], for: .selected)
        
        UITabBarItem.appearance().setTitleTextAttributes([NSAttributedString.Key.foregroundColor:UIColor.black], for: .normal)
        startDate.text = "Start Date"
        endDate.text = "End Date"
        startDate.textColor = .lightGray
        endDate.textColor = .lightGray
        errorLabel.isHidden = true
        getData()
        
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        UITabBarItem.appearance().setTitleTextAttributes([NSAttributedString.Key.foregroundColor:UIColor(hexString: "#162760")], for: .selected)

        UITabBarItem.appearance().setTitleTextAttributes([NSAttributedString.Key.foregroundColor:UIColor.black], for: .normal)
        startDate.text = "Start Date"
        endDate.text = "End Date"
        startDate.textColor = .lightGray
        endDate.textColor = .lightGray
        errorLabel.isHidden = true
        getData()
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        var count = 0
        if filteredArray.count > 0 {
//            print("count in filter cell \(filteredArray.count)")
            count = filteredArray.count
        }
        else {
//            print("count in cell \(logObj.count)")
            count = logObj.count
        }
        return count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        var cell1 = UITableViewCell()
         let logData = tableView.dequeueReusableCell(withIdentifier: "logCell", for: indexPath) as! LogTableViewCell
       
        if filteredArray.count > 0 {
           
//            logData.date.backgroundColor = UIColor(red: 118, green: 214, blue: 255, alpha: 1)
            let originalDateFormatter = DateFormatter()
            originalDateFormatter.dateFormat = "dd-MM-yyyy"
//            "yyyy-MM-dd"
            let newDateFormatter = DateFormatter()
            newDateFormatter.dateFormat = "dd-MMM"
            if let date: Date = originalDateFormatter.date(from: filteredArray[indexPath.row].date!) {
                let dateInNewStringFormat: String = newDateFormatter.string(from: date)
//                print("dateInNewStringFormat\(dateInNewStringFormat)")
            logData.date.text = dateInNewStringFormat
            logData.date.backgroundColor = UIColor(hexString: "#87CEEB")
            }
            logData.time.text = filteredArray[indexPath.row].time
            logData.sys.text = filteredArray[indexPath.row].systa
            logData.dia.text = filteredArray[indexPath.row].diasta
            logData.rate.text = filteredArray[indexPath.row].heartRate
            cell1 = logData
        
        }
        else {
//                logData.date.backgroundColor = UIColor(red: 118, green: 214, blue: 255, alpha: 1)
            let originalDateFormatter = DateFormatter()
            originalDateFormatter.dateFormat = "dd-MM-yyyy"
//            "yyyy-MM-dd"
            let newDateFormatter = DateFormatter()
            newDateFormatter.dateFormat = "dd-MMM"
                if let date: Date = originalDateFormatter.date(from: logObj[indexPath.row].date!) {
                    let dateInNewStringFormat: String = newDateFormatter.string(from: date)
//                    print("dateInNewStringFormat\(dateInNewStringFormat)")
            logData.date.text = dateInNewStringFormat
            logData.date.backgroundColor = UIColor(hexString: "#87CEEB")
            }
            logData.time.text = logObj[indexPath.row].time
            logData.sys.text = logObj[indexPath.row].systolic
            logData.dia.text = logObj[indexPath.row].diastolic
            logData.rate.text = logObj[indexPath.row].heartRate
            cell1 = logData
            }
        
        return cell1
    }
    
    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return 50
    }
    
    // To get data from coredata and append it to model
    func getData() {
        localReadingList.removeAll()
        filteredArray.removeAll()
        do {
            let context = (UIApplication.shared.delegate as! AppDelegate).persistentContainer.viewContext
            logObj = try context.fetch(Reading.fetchRequest())
//            print("logobj \(logObj.count)")
            if logObj.count > 0 {
                for i in logObj {
//                    print("name \(i.name)")
                    self.logTableView.isHidden = false
                    self.errorLabel.isHidden = true
                    let obj = readingData(myname: i.name!, mysysta: i.systolic!, myDiasta: i.diastolic!, myRate: i.heartRate!, mymap: i.map!, myDate: i.date!, myTime: i.time!, myIrregular: i.irregularHB!)
                    localReadingList.append(obj)
//                    print("count \(localReadingList.count)")
                    self.logTableView.reloadData()
                }
//                self.logTableView.reloadData()
            }
            else {
                self.logTableView.isHidden = true
                self.errorLabel.isHidden = false
                self.errorLabel.text = "No Data Found"
                self.logTableView.setEmptyMessage("No Data Found")
            }
        }
        catch {
            print("failed")
        }
    }
    
    // Calendar popup - Start date calendar
    @IBAction func startCalendar(_ sender: UIButton) {
        
        let storyboard : UIStoryboard = UIStoryboard(name: "Main", bundle: nil)
        let popover = storyboard.instantiateViewController(withIdentifier: "calendarView1") as! CalendarStartDateSelectViewController
        popover.delegate1 = self
        
        popover.modalPresentationStyle = UIModalPresentationStyle.popover
        //        popover.popoverPresentationController?.backgroundColor = UIColor.green
        popover.popoverPresentationController?.delegate = self
        
        popover.popoverPresentationController?.sourceRect = CGRect(x: view.center.x, y: view.center.y, width: 0, height: 0)
        popover.popoverPresentationController?.sourceView = view
        popover.popoverPresentationController?.permittedArrowDirections = UIPopoverArrowDirection(rawValue: 0)
        self.present(popover, animated: true)
        
    }
    
//    End date calendar
    @IBAction func endCalendar(_ sender: UIButton) {
        let storyboard : UIStoryboard = UIStoryboard(name: "Main", bundle: nil)
        let popover = storyboard.instantiateViewController(withIdentifier: "calendarView") as! CalendarSelectViewController
        popover.delegate = self
        popover.modalPresentationStyle = UIModalPresentationStyle.popover
        //        popover.popoverPresentationController?.backgroundColor = UIColor.green
        popover.popoverPresentationController?.delegate = self
        
        popover.popoverPresentationController?.sourceRect = CGRect(x: view.center.x, y: view.center.y, width: 0, height: 0)
        popover.popoverPresentationController?.sourceView = view
        popover.popoverPresentationController?.permittedArrowDirections = UIPopoverArrowDirection(rawValue: 0)
        self.present(popover, animated: true)
    }
    
    func adaptivePresentationStyle(for controller: UIPresentationController) -> UIModalPresentationStyle {
        return UIModalPresentationStyle.none
    }
    
    // Filter the date and display data
    @IBAction func applyDateFilter(_ sender: UIButton) {
        if (startDate.text == "" || startDate.text == "Start Date"){
            alert(title: "Alert!", msg: "Please select start date to continue", buttonName: "OK")
        }
        else if (endDate.text == "" || endDate.text == "End Date") {
            alert(title: "Alert!", msg: "Please select end date to continue", buttonName: "OK")
        }
        else {
            dateRange(strstartDate: selectedStartDate!, strendDate: selectedEndDate!)
        }
    }
    
    func alert(title:String, msg:String,buttonName:String) {
        let alert = UIAlertController(title: title, message: msg, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: buttonName, style: .default, handler: nil))
        present(alert, animated: true, completion: nil)
    }
    
    func dateRange(strstartDate:String, strendDate:String)
    {
//        print("filter date")
        selectedDateArray.removeAll()
        filteredArray.removeAll()
        var dateFrom =  Date() // First date
        var dateTo = Date() // End date
        let fmt = DateFormatter()
        fmt.dateFormat = "dd-MM-yyyy"
//        "yyyy-MM-dd"
        dateFrom = fmt.date(from: strstartDate)! // user selected start date
        dateTo = fmt.date(from: strendDate)! // user selected End date
        
        // Appending dates to array
        while dateFrom <= dateTo {
            selectedDateArray.append(fmt.string(from: dateFrom))
            dateFrom = Calendar.current.date(byAdding: .day, value: 1, to: dateFrom)!
        }
//        print("selected date \(selectedDateArray) date from \(dateFrom)")
        
        // Adding filtered data to the model
            if localReadingList.count > 0
            {
                for i in localReadingList
                {
                    if selectedDateArray.count > 0
                    {
                        for j in selectedDateArray
                        {
                            print("date \(String(describing: i.date)) and \(j)")
                            if i.date == j
                            {
//                               print("name \(i.name)")
                                self.errorLabel.isHidden = true
                                self.logTableView.isHidden = false
                                let obj = filteredData(myname: i.name!, mysysta: i.systa!, myDiasta: i.diasta!, myRate: i.heartRate!, mymap: i.map!, myDate: i.date!, myTime: i.time!, myIrregular: i.irregular!)
                                filteredArray.append(obj)
//                                print("filter array \(filteredArray)")
                                
//                                self.logTableView.reloadData()
                            }
                        }
                    }
                    else
                    {
                        self.logTableView.isHidden = true
                        self.errorLabel.isHidden = false
                        self.errorLabel.text = "No Data Found"
//                        self.logTableView.setEmptyMessage("No Data Found")
//                        print("filter array in count else \(filteredArray)")
                    }
                }
                
                if filteredArray.count > 0 {
                    self.logTableView.reloadData()
                }
                else {
                    self.logTableView.isHidden = true
                    self.errorLabel.isHidden = false
                    self.errorLabel.text = "No Data Found"
  //                self.logTableView.setEmptyMessage("No Data Found")
                }
            }
        
        //        print(selectedDateArray)
    }
    
    // Formatting the date
    func formattedDateFromString(dateString: String, withFormat format: String) -> String? {
        
        let inputFormatter = DateFormatter()
        inputFormatter.dateFormat = "dd-MM-yyyy"
        
        if let date = inputFormatter.date(from: dateString) {
            
            let outputFormatter = DateFormatter()
            outputFormatter.dateFormat = format
            
            return outputFormatter.string(from: date)
        }
        
        return nil
    }
    
    //To share readings
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let index = indexPath.row
        let storyboard : UIStoryboard = UIStoryboard(name: "Main", bundle: nil)
        let popover = storyboard.instantiateViewController(withIdentifier: "shareReadings") as! ShareReadingsViewController
        if filteredArray.count > 0 {
            popover.selectedDate = filteredArray[index].date
            popover.selectedSystolic = filteredArray[index].systa
            popover.selectedDiastolic = filteredArray[index].diasta
            popover.selectedHeartRate = filteredArray[index].heartRate
            popover.selectedIrregular = filteredArray[index].irregular
        }
        else {
            popover.selectedDate = logObj[index].date
            popover.selectedSystolic = logObj[index].systolic
            popover.selectedDiastolic = logObj[index].diastolic
            popover.selectedHeartRate = logObj[index].heartRate
            popover.selectedIrregular = logObj[index].irregularHB
            }
//        popover.delegate = self
        
        popover.modalPresentationStyle = UIModalPresentationStyle.popover
        //        popover.popoverPresentationController?.backgroundColor = UIColor.green
        popover.popoverPresentationController?.delegate = self
        
        popover.popoverPresentationController?.sourceRect = CGRect(x: view.center.x, y: view.center.y, width: 0, height: 0)
        popover.popoverPresentationController?.sourceView = view
        popover.popoverPresentationController?.permittedArrowDirections = UIPopoverArrowDirection(rawValue: 0)
        self.present(popover, animated: true)

    }
    
}

extension Date {
    static func dates(from fromDate: Date, to toDate: Date) -> [Date] {
        var dates: [Date] = []
        var date = fromDate
        
        while date <= toDate {
            dates.append(date)
            guard let newDate = Calendar.current.date(byAdding: .day, value: 1, to: date) else { break }
            date = newDate
        }
        return dates
    }
}

// for error message in tableview
extension UITableView {
    
    func setEmptyMessage(_ message: String) {
        let messageLabel = UILabel(frame: CGRect(x: 0, y: 0, width: self.bounds.size.width, height: self.bounds.size.height))
        messageLabel.text = message
        messageLabel.textColor = .black
        messageLabel.numberOfLines = 0
        messageLabel.textAlignment = .center
        messageLabel.font = UIFont(name: "TrebuchetMS", size: 12)
        messageLabel.sizeToFit()
        
        self.backgroundView = messageLabel
        self.separatorStyle = .none
    }
    
    func restore() {
        self.backgroundView = nil
        self.separatorStyle = .singleLine
    }
}
