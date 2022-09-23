//
//  ViewController.swift
//  BP monitor app
//
//  Created by fueb on 27/06/22.
//  Copyright © 2022 fueb. All rights reserved.
//
//https://medium.com/@amilaindrajithkarunaratne/ios-swift-5-get-click-event-for-uiimageview-6d0f331c3bca - imageview tapguesture
//https://www.raywenderlich.com/25358187-spinner-and-progress-bar-in-swift-getting-started - progress view


import UIKit
import Charts

class ViewController: UIViewController, ChartViewDelegate, startdateSelect, UIPopoverPresentationControllerDelegate {
    
    
    @IBOutlet var profilePic: UIImageView!
    @IBOutlet var backgroundImg: UIImageView!
    var readingsObj:[Reading] = []
    var filteredObj:[Reading] = []
    let markerView = CustomMarkerView()
    
    @IBOutlet var bpReadings: UILabel!
    @IBOutlet var heartRateReadings: UILabel!
//    @IBOutlet var progress1: UIProgressView!
//    @IBOutlet var progress2: UIProgressView!
    @IBOutlet var progress1: UISlider!
    var datelist:[String] = []
    var sysList:[Int] = []
    var diaList:[Int] = []
    @IBOutlet var activityView1: UIView!
    @IBOutlet var activityIndicator1: UIActivityIndicatorView!
    @IBOutlet var progress2: UISlider!
    
    @IBOutlet var combinedChart: CombinedChartView!
    //    @IBOutlet var lineChart: LineChartView!
    //    @IBOutlet var candleStick: CandleStickChartView!
    @IBOutlet var dateChangeLabel: UILabel!
    
    var dateSelected:String?
    
    //Adding selected date to label and plotting the graph as per the date
    func selectedDate(date: String) {
        combinedChart.clear()
        datelist.removeAll()
        sysList.removeAll()
        diaList.removeAll()
//        activityView.isHidden = true
//        activityIndicator.stopAnimating()
        // Changing date format.
        let originalDateFormatter = DateFormatter()
        originalDateFormatter.dateFormat = "dd-MM-yyyy"
        //            "yyyy-MM-dd"
        let newDateFormatter = DateFormatter()
        newDateFormatter.dateFormat = "dd-MMM"
        if let date: Date = originalDateFormatter.date(from: date) {
            let dateInNewStringFormat: String = newDateFormatter.string(from: date)
            dateChangeLabel.text = dateInNewStringFormat
        }
        
        dateSelected = date
        
        if filteredObj.count > 0 {
            for i in filteredObj {
                if i.date == dateSelected {
                    datelist.append(i.time!)
                    sysList.append(Int(i.systolic!)!)
                    diaList.append(Int(i.diastolic!)!)
                    convertCombines(dataEntryX: datelist, dataEntryY: sysList, dataEntryZ: diaList)
                }
                else {
                    combinedChart.noDataText = "Please provide data to the chart"
                    combinedChart.notifyDataSetChanged()
                }
            }
        }
        else {
            combinedChart.noDataText = "Please provide data to the chart"
            combinedChart.notifyDataSetChanged()
        }
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
//        dateChangeLabel.text = ""
        self.navigationController?.navigationItem.hidesBackButton = true
        navigationItem.setHidesBackButton(true, animated: false)
        self.tabBarController?.tabBar.isHidden = false
        
        UITabBarItem.appearance().setTitleTextAttributes([NSAttributedString.Key.foregroundColor:UIColor(hexString: "#162760")], for: .selected)
                
        UITabBarItem.appearance().setTitleTextAttributes([NSAttributedString.Key.foregroundColor:UIColor.black], for: .normal)
        
//        activityView1.isHidden = false
//        activityIndicator1.startAnimating()
        getData()
         // Tap guesture to navigate to next vc from imageview
//        let tapGestureRecognizer = UITapGestureRecognizer(target: self, action: #selector(imageViewTapped(_:)))
//
//        backgroundImg.isUserInteractionEnabled = true
//
//        backgroundImg.addGestureRecognizer(tapGestureRecognizer)
        
        profilePic.layer.masksToBounds = true
        profilePic.layer.cornerRadius = profilePic.bounds.width / 2
        
//        markerView.chartView = combinedChart
//        combinedChart.marker = markerView as! IMarker
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        getData()
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        getData()
    }
    
     // Navigate to next vc from imageview
//    @objc func imageViewTapped(_ sender:AnyObject){
//
////        print("imageview tapped")
//
//        performSegue(withIdentifier: "toDeviceConnection", sender: self)
//
//    }

    // To get data from coredata and append it to model & plot the graph
    func getData() {
//        localReadingList.removeAll()
        do {
            let context = (UIApplication.shared.delegate as! AppDelegate).persistentContainer.viewContext
            combinedChart.clear()
            filteredObj.removeAll()
            datelist.removeAll()
            sysList.removeAll()
            diaList.removeAll()
            readingsObj = try context.fetch(Reading.fetchRequest())
            //            print("logobj \(logObj.count)")
            if readingsObj.count > 0 {
                //Fetching latest reading from array.
                if let lastElement = readingsObj.last {
//                    print("Last element \(lastElement.systolic)")
                    bpReadings.text = lastElement.systolic! + " / " + lastElement.diastolic! + " mmHg"
                    heartRateReadings.text = lastElement.heartRate! + " bpm"
                    
                    let originalDateFormatter = DateFormatter()
                    originalDateFormatter.dateFormat = "dd-MM-yyyy"
                    //            "yyyy-MM-dd"
                    let newDateFormatter = DateFormatter()
                    newDateFormatter.dateFormat = "dd - MMM"
                    if let date: Date = originalDateFormatter.date(from: lastElement.date!) {
                        let dateInNewStringFormat: String = newDateFormatter.string(from: date)
//                        print("dateInNewStringFormat\(dateInNewStringFormat)")
//                        logData.date.text = dateInNewStringFormat
//                         xVal = dateInNewStringFormat + "\n" + i.time!
                        dateChangeLabel.text = dateInNewStringFormat
                    }
                    
                    if ((lastElement.systolic == "-") || (lastElement.diastolic == "-")) {
                        print("not an format")
                    }
                    else {
                        changeSystolicProgress(systolic1: Int(lastElement.systolic!)!)
                        changeDiastolicProgress(diastolic1: Int(lastElement.diastolic!)!)
                    }
                }
                
                for i in readingsObj {
                    var xVal:String?
                    let originalDateFormatter = DateFormatter()
                    originalDateFormatter.dateFormat = "dd-MM-yyyy"
                    //            "yyyy-MM-dd"
                    let newDateFormatter = DateFormatter()
                    newDateFormatter.dateFormat = "dd - MMM"
                    if let date: Date = originalDateFormatter.date(from: i.date!) {
                        let dateInNewStringFormat: String = newDateFormatter.string(from: date)
//                        print("dateInNewStringFormat\(dateInNewStringFormat)")
//                        logData.date.text = dateInNewStringFormat
                         xVal = dateInNewStringFormat + "\n" + i.time!
                        dateChangeLabel.text = "16 - Sep"
                    }
                    
//                    print("xVal = \(xVal)")
                    filteredObj.append(i)
                    datelist.append(xVal!)
                    sysList.append(Int(i.systolic!)!)
                    diaList.append(Int(i.diastolic!)!)
                    
                }
//                setChart(xValues: datelist, systolic: sysList, diastolic: diaList)
                convertCombines(dataEntryX: datelist, dataEntryY: sysList, dataEntryZ: diaList)
//                activityView1.isHidden = true
//                activityIndicator1.stopAnimating()
            }
            else {
                combinedChart.noDataText = "Please provide data to the chart"
//                activityView1.isHidden = true
//                activityIndicator1.stopAnimating()
            }
        }
        catch {
            print("failed")
        }
    }
    
    //Updating progress bar according to systolic and diastolic readings
    func changeSystolicProgress(systolic1:Int) {
        if (systolic1 < 80) {
            progress1.value = Float(systolic1)
            progress1.tintColor = UIColor(hexString: "#90EE90")
        }
        else if (systolic1 >= 80 && systolic1 < 120) {
            progress1.value = Float(systolic1)
            progress1.tintColor = UIColor(hexString: "#008000")
        }
        else if (systolic1 >= 120 && systolic1 < 139) {
            progress1.value = Float(systolic1)
            progress1.tintColor = UIColor(hexString: "#FFD700")
        }
        else if (systolic1 >= 139 && systolic1 < 159) {
            progress1.value = Float(systolic1)
            progress1.tintColor = UIColor(hexString: "#FFA500")
        }
        else if (systolic1 >= 159 && systolic1 < 179) {
            progress1.value = Float(systolic1)
            progress1.tintColor = UIColor(hexString: "#FF8C00")
        }
        else {
            progress1.value = Float(systolic1)
            progress1.tintColor = UIColor(hexString: "#FF0000")
        }
    }
    
    func changeDiastolicProgress(diastolic1:Int) {
        if diastolic1 < 60 {
            progress2.value = Float(diastolic1)
            progress2.tintColor =  UIColor(hexString: "#90EE90")
        }
        else if (diastolic1 >= 60 && diastolic1 < 80) {
            progress2.value = Float(diastolic1)
            progress2.tintColor =  UIColor(hexString: "#008000")
        }
        else if (diastolic1 >= 80 && diastolic1 < 89) {
            progress2.value = Float(diastolic1)
            progress2.tintColor =  UIColor(hexString: "#FFD700")
        }
        else if (diastolic1 >= 89 && diastolic1 < 99) {
            progress2.value = Float(diastolic1)
            progress2.tintColor =  UIColor(hexString: "#FFA500")
        }
        else if (diastolic1 >= 99 && diastolic1 < 109) {
            progress2.value = Float(diastolic1)
            progress2.tintColor = UIColor(hexString: "#FF8C00")
        }
        else {
            progress2.value = Float(diastolic1)
            progress2.tintColor =  UIColor(hexString: "#FF0000")
        }
    }
    
    // Plot graph on tap of all button
    @IBAction func allViewBtn(_ sender: UIButton) {
        combinedChart.clear()
        datelist.removeAll()
        sysList.removeAll()
        diaList.removeAll()
        if filteredObj.count > 0 {
            
            //Fetching latest reading from array.
            if let lastElement = filteredObj.last {
                
                let originalDateFormatter = DateFormatter()
                originalDateFormatter.dateFormat = "dd-MM-yyyy"
                //            "yyyy-MM-dd"
                let newDateFormatter = DateFormatter()
                newDateFormatter.dateFormat = "dd - MMM"
                if let date: Date = originalDateFormatter.date(from: lastElement.date!) {
                    let dateInNewStringFormat: String = newDateFormatter.string(from: date)
//                        print("dateInNewStringFormat\(dateInNewStringFormat)")
//                        logData.date.text = dateInNewStringFormat
//                         xVal = dateInNewStringFormat + "\n" + i.time!
                    dateChangeLabel.text = dateInNewStringFormat
                }
            }
            
            for i in filteredObj {
                var xVal:String?
                let originalDateFormatter = DateFormatter()
                originalDateFormatter.dateFormat = "dd-MM-yyyy"
                //            "yyyy-MM-dd"
                let newDateFormatter = DateFormatter()
                newDateFormatter.dateFormat = "dd-MMM"
                if let date: Date = originalDateFormatter.date(from: i.date!) {
                    let dateInNewStringFormat: String = newDateFormatter.string(from: date)
//                    print("dateInNewStringFormat\(dateInNewStringFormat)")
//                    logData.date.text = dateInNewStringFormat
                    xVal = dateInNewStringFormat + "\n" + i.time!
                    dateChangeLabel.text = "16 - Sep"
                }
                datelist.append(xVal!)
                sysList.append(Int(i.systolic!)!)
                diaList.append(Int(i.diastolic!)!)
            }
//            print("date list \(datelist)")
            convertCombines(dataEntryX: datelist, dataEntryY: sysList, dataEntryZ: diaList)
//            activityView1.isHidden = true
//            activityIndicator1.stopAnimating()
        }
        else {
            combinedChart.noDataText = "Please provide data to the chart"
            combinedChart.notifyDataSetChanged()
//            activityView1.isHidden = true
//            activityIndicator1.stopAnimating()
        }
    }
    
    // calendar popup
    @IBAction func calendarViewBtn(_ sender: UIButton) {
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
    
    func adaptivePresentationStyle(for controller: UIPresentationController) -> UIModalPresentationStyle {
        return UIModalPresentationStyle.none
    }
    
    // Combinedchart with Line and candlestick
    func convertCombines(dataEntryX forX:[String],dataEntryY forY: [Int], dataEntryZ forZ: [Int]) {
    
        activityView1.isHidden = true
        activityIndicator1.stopAnimating()
        var dataEntries: [CandleChartDataEntry] = []
        var dataEntrieszor: [ChartDataEntry] = [ChartDataEntry]()
        var dataEntries1: [ChartDataEntry] = [ChartDataEntry]()
        let data = LineChartData()
        
        if forX.count == 1 {
            for (i, v) in forY.enumerated() {
                let dataEntry = CandleChartDataEntry(x: Double(i), shadowH: Double(v), shadowL: Double(diaList[i]), open: Double(v), close: Double(diaList[i]), data: forX as AnyObject?)
                dataEntries.append(dataEntry)
            }
            
            for i in 0..<forX.count{
                let dataEntry = ChartDataEntry(x: Double(i), y: Double(forY[i]), data: forX as AnyObject?)
                dataEntrieszor.append(dataEntry)
                let dataEntry1 = ChartDataEntry(x: Double(i), y: Double(forZ[i]), data: forX as AnyObject?)
                dataEntries1.append(dataEntry1)
            }
            
            
            let lineChartSet = LineChartDataSet(entries: dataEntrieszor)
            data.addDataSet(lineChartSet)
            let lineChartSet1 = LineChartDataSet(entries: dataEntries1)
            data.addDataSet(lineChartSet1)
     
            //Ui
            let candleChartSet = CandleChartDataSet(entries: dataEntries)
            let candleChartData = CandleChartData(dataSet: candleChartSet)
            candleChartSet.drawValuesEnabled = false
            candleChartSet.barSpace = 10
            candleChartSet.shadowColor = UIColor.darkGray
            candleChartSet.decreasingColor = UIColor(hexString: "#FFA500")
            candleChartSet.decreasingFilled = true
            candleChartSet.increasingColor = UIColor(hexString: "#FFA500")
            candleChartSet.increasingFilled = false
            candleChartSet.setDrawHighlightIndicators(false)
            candleChartSet.neutralColor = UIColor.blue
            candleChartSet.setColor(UIColor.init(red: 80, green: 80, blue: 80, alpha: 1))
            candleChartSet.drawValuesEnabled = false
            
            //Ui
            lineChartSet.setColor(UIColor.magenta)
            lineChartSet.setCircleColor(UIColor(hexString: "#50EBEC"))
            lineChartSet.circleRadius = 5
            lineChartSet1.setColor(UIColor.red)
            lineChartSet1.setCircleColor(UIColor(hexString: "#50EBEC"))
            lineChartSet1.circleRadius = 5
            lineChartSet.lineWidth = 2
            lineChartSet.lineDashLengths = [3]
            lineChartSet.drawCircleHoleEnabled = false
            lineChartSet1.lineWidth = 2
            lineChartSet1.lineDashLengths = [3]
            lineChartSet1.drawCircleHoleEnabled = false
            lineChartSet.drawValuesEnabled = true
            lineChartSet.setDrawHighlightIndicators(false)
            lineChartSet1.drawValuesEnabled = true
            lineChartSet1.setDrawHighlightIndicators(false)
            
    //        let comData = CombinedChartData(dataSets: [lineChartSet,lineChartSet1,candleChartSet])
            let comData = CombinedChartData()
    //        comData.barData = barChartData
            comData.candleData = candleChartData
            comData.lineData = data
            
    //         combinedChart.setVisibleXRangeMaximum(5)
            if(dataEntries1.count >= 5) {
                combinedChart.setVisibleXRangeMaximum(5)
            }

            if(dataEntrieszor.count >= 5) {
                combinedChart.setVisibleXRangeMaximum(5)
            }

            if(dataEntries.count >= 5) {
                combinedChart.setVisibleXRangeMaximum(5)
            }

            if dataEntries1.count > 1 {
                combinedChart.moveViewToX(Double(datelist.count-1))
            }
            if dataEntries.count > 1 {
                combinedChart.moveViewToX(Double(datelist.count-1))
            }
            if dataEntrieszor.count > 1 {
                combinedChart.moveViewToX(Double(datelist.count-1))
            }
            
            combinedChart.data = comData
            combinedChart.notifyDataSetChanged()
            
            combinedChart.legend.enabled = false
        
    //        print("forx \(forX.count)")
            //xAxis
            combinedChart.xAxis.valueFormatter = IndexAxisValueFormatter(values:forX)
            combinedChart.xAxis.granularity = 1
            combinedChart.xAxis.granularityEnabled = true
            combinedChart.xAxis.setLabelCount(forX.count, force: false)
            combinedChart.xAxis.labelFont = UIFont.systemFont(ofSize: 8.0, weight: UIFont.Weight.regular)
            combinedChart.xAxis.labelRotationAngle = -45
            combinedChart.xAxis.labelTextColor = .black
            combinedChart.xAxis.spaceMax = 0.45
            combinedChart.xAxis.spaceMin = 0.5
            combinedChart.xAxis.axisMinimum = -0.5
            combinedChart.xAxis.avoidFirstLastClippingEnabled = true
            combinedChart.xAxis.labelPosition = .bottom
            combinedChart.zoom(scaleX: 1.0, scaleY: 1.0, x: 0.0, y: 0.0)
            combinedChart.xAxis.drawAxisLineEnabled = false
            combinedChart.xAxis.drawGridLinesEnabled = false
            combinedChart.xAxis.wordWrapEnabled = true
            combinedChart.xAxis.centerAxisLabelsEnabled = false
            
            //y axis
            combinedChart.rightAxis.drawGridLinesEnabled = true
    //        combinedChart.rightAxis.drawGridLinesEnabled = false
            combinedChart.rightAxis.drawLabelsEnabled = false
            combinedChart.leftAxis.drawGridLinesEnabled = false
            combinedChart.leftAxis.drawAxisLineEnabled = false
            combinedChart.leftAxis.drawZeroLineEnabled = true
            combinedChart.leftAxis.labelCount = 5
            combinedChart.leftAxis.axisMinimum = 50
            combinedChart.leftAxis.axisMaximum = 200
            combinedChart.leftAxis.labelFont = UIFont.systemFont(ofSize: 8.0, weight: UIFont.Weight.regular)
            combinedChart.leftAxis.labelPosition = .outsideChart
            combinedChart.leftAxis.labelTextColor = .black
            let marker = ChartMarker()
            marker.chartView = combinedChart
            combinedChart.marker = marker
            
            combinedChart.pinchZoomEnabled = true
            combinedChart.dragEnabled = true
    //        combinedChart.isMultipleTouchEnabled = true
            combinedChart.scaleXEnabled = true
            combinedChart.scaleYEnabled = false
            
            combinedChart.animate(xAxisDuration: 2.0, yAxisDuration: 2.0, easingOption: .easeInCirc)
            
        }
        else {
            for (i, v) in forY.enumerated() {
    //            let dataEntry = BarChartDataEntry(x: Double(i), y: Double(v), data: forX as AnyObject?)
                let dataEntry = CandleChartDataEntry(x: Double(i), shadowH: Double(v), shadowL: Double(diaList[i]), open: Double(v), close: Double(diaList[i]), data: forX as AnyObject?)
                dataEntries.append(dataEntry)
            }
            
            for i in 0..<forX.count{
                let dataEntry = ChartDataEntry(x: Double(i), y: Double(forY[i]), data: forX as AnyObject?)
                dataEntrieszor.append(dataEntry)
                let dataEntry1 = ChartDataEntry(x: Double(i), y: Double(forZ[i]), data: forX as AnyObject?)
                dataEntries1.append(dataEntry1)
    //            print("entries \( dataEntrieszor) \( dataEntries1)")
            }
            
            
            let lineChartSet = LineChartDataSet(entries: dataEntrieszor)
            data.addDataSet(lineChartSet)
            let lineChartSet1 = LineChartDataSet(entries: dataEntries1)
            data.addDataSet(lineChartSet1)
     
    //        let barChartSet = BarChartDataSet(entries: dataEntries, label: "Bar Data")
    //        let barChartData = BarChartData(dataSets: [barChartSet])
            let candleChartSet = CandleChartDataSet(entries: dataEntries)
            let candleChartData = CandleChartData(dataSet: candleChartSet)
            candleChartSet.drawValuesEnabled = false
            candleChartSet.barSpace = 10
            candleChartSet.shadowColor = UIColor.darkGray
            candleChartSet.decreasingColor = UIColor(hexString: "#FFA500")
            candleChartSet.decreasingFilled = true
            candleChartSet.increasingColor = UIColor(hexString: "#FFA500")
            candleChartSet.increasingFilled = false
            candleChartSet.setDrawHighlightIndicators(false)
            candleChartSet.neutralColor = UIColor.blue
            candleChartSet.setColor(UIColor.init(red: 80, green: 80, blue: 80, alpha: 1))
            candleChartSet.drawValuesEnabled = false
            
            //ui
            lineChartSet.setColor(UIColor.magenta)
            lineChartSet.setCircleColor(UIColor(hexString: "#50EBEC"))
            lineChartSet.circleRadius = 5
            lineChartSet1.setColor(UIColor.red)
            lineChartSet1.setCircleColor(UIColor(hexString: "#50EBEC"))
            lineChartSet1.circleRadius = 5
            lineChartSet.lineWidth = 2
            lineChartSet.lineDashLengths = [3]
            lineChartSet.drawCircleHoleEnabled = false
            lineChartSet1.lineWidth = 2
            lineChartSet1.lineDashLengths = [3]
            lineChartSet1.drawCircleHoleEnabled = false
            lineChartSet.drawValuesEnabled = true
            lineChartSet.setDrawHighlightIndicators(false)
            lineChartSet1.drawValuesEnabled = true
            lineChartSet1.setDrawHighlightIndicators(false)
            
    //        let comData = CombinedChartData(dataSets: [lineChartSet,lineChartSet1,candleChartSet])
            let comData = CombinedChartData()
    //        comData.barData = barChartData
            comData.candleData = candleChartData
            comData.lineData = data
            
    //         combinedChart.setVisibleXRangeMaximum(5)
            if(dataEntries1.count >= 5) {
                combinedChart.setVisibleXRangeMaximum(5)
            }

            if(dataEntrieszor.count >= 5) {
                combinedChart.setVisibleXRangeMaximum(5)
            }

            if(dataEntries.count >= 5) {
                combinedChart.setVisibleXRangeMaximum(5)
            }

            if dataEntries1.count > 1 {
                combinedChart.moveViewToX(Double(datelist.count-1))
            }
            if dataEntries.count > 1 {
                combinedChart.moveViewToX(Double(datelist.count-1))
            }
            if dataEntrieszor.count > 1 {
                combinedChart.moveViewToX(Double(datelist.count-1))
            }
            
            combinedChart.data = comData
            combinedChart.notifyDataSetChanged()
            
            combinedChart.legend.enabled = false
        
//            print("forx \(forX.count)")
            
            //xAxis
            combinedChart.xAxis.valueFormatter = IndexAxisValueFormatter(values:forX)
            combinedChart.xAxis.granularity = 1
            combinedChart.xAxis.granularityEnabled = true
            combinedChart.xAxis.labelCount = forX.count
//            combinedChart.xAxis.setLabelCount(forX.count, force: true)
            combinedChart.xAxis.labelFont = UIFont.systemFont(ofSize: 8.0, weight: UIFont.Weight.regular)
            combinedChart.xAxis.labelRotationAngle = -45
            combinedChart.xAxis.labelTextColor = .black
            combinedChart.xAxis.spaceMax = 0.45
            combinedChart.xAxis.spaceMin = 0.5
            combinedChart.xAxis.axisMinimum = -0.5
            combinedChart.xAxis.avoidFirstLastClippingEnabled = true
            combinedChart.xAxis.labelPosition = .bottom
            combinedChart.zoom(scaleX: 1.0, scaleY: 1.0, x: 0.0, y: 0.0)
            combinedChart.xAxis.drawAxisLineEnabled = false
            combinedChart.xAxis.drawGridLinesEnabled = false
            combinedChart.xAxis.wordWrapEnabled = true
            combinedChart.xAxis.centerAxisLabelsEnabled = false
            
            //y axis
            combinedChart.rightAxis.drawGridLinesEnabled = true
    //        combinedChart.rightAxis.drawGridLinesEnabled = false
            combinedChart.rightAxis.drawLabelsEnabled = false
            combinedChart.leftAxis.drawGridLinesEnabled = false
            combinedChart.leftAxis.drawAxisLineEnabled = false
            combinedChart.leftAxis.drawZeroLineEnabled = true
            combinedChart.leftAxis.labelCount = 5
            combinedChart.leftAxis.axisMinimum = 50
            combinedChart.leftAxis.axisMaximum = 200
            combinedChart.leftAxis.labelFont = UIFont.systemFont(ofSize: 8.0, weight: UIFont.Weight.regular)
            combinedChart.leftAxis.labelPosition = .outsideChart
            combinedChart.leftAxis.labelTextColor = .black
            
            let marker = ChartMarker()
            marker.chartView = combinedChart
            combinedChart.marker = marker
            
            let xRenderer = CustomXAxisRenderer(viewPortHandler: combinedChart.viewPortHandler, xAxis: combinedChart.xAxis,transformer: combinedChart.getTransformer(forAxis: YAxis.AxisDependency.left))
            combinedChart.xAxisRenderer = xRenderer
//            print("xaxis \(String(describing: xRenderer.scrollDate))")
//            dateChangeLabel.text = xRenderer.scrollDate
            
            combinedChart.pinchZoomEnabled = true
            combinedChart.dragEnabled = true
    //        combinedChart.isMultipleTouchEnabled = true
            combinedChart.scaleXEnabled = true
            combinedChart.scaleYEnabled = false
            
            combinedChart.animate(xAxisDuration: 2.0, yAxisDuration: 2.0, easingOption: .easeInCirc)
        }
    }
    }

extension UIColor {
    convenience init(hexString: String, alpha: CGFloat = 1.0) {
        let hexString: String = hexString.trimmingCharacters(in: CharacterSet.whitespacesAndNewlines)
        let scanner = Scanner(string: hexString)
        if (hexString.hasPrefix("#")) {
            scanner.scanLocation = 1
        }
        var color: UInt32 = 0
        scanner.scanHexInt32(&color)
        let mask = 0x000000FF
        let r = Int(color >> 16) & mask
        let g = Int(color >> 8) & mask
        let b = Int(color) & mask
        let red   = CGFloat(r) / 255.0
        let green = CGFloat(g) / 255.0
        let blue  = CGFloat(b) / 255.0
        self.init(red:red, green:green, blue:blue, alpha:alpha)
    }
    func toHexString() -> String {
        var r:CGFloat = 0
        var g:CGFloat = 0
        var b:CGFloat = 0
        var a:CGFloat = 0
        getRed(&r, green: &g, blue: &b, alpha: &a)
        let rgb:Int = (Int)(r*255)<<16 | (Int)(g*255)<<8 | (Int)(b*255)<<0
        return String(format:"#%06x", rgb)
    }
}

extension ViewController {
    
    class CustomXAxisRenderer: XAxisRenderer {
        var scrollDate:String?
        override init(viewPortHandler: ViewPortHandler, xAxis: XAxis?, transformer: Transformer?) {
            super.init(viewPortHandler: viewPortHandler, xAxis: xAxis, transformer: transformer)
        }

        override func drawLabel(context: CGContext, formattedLabel: String, x: CGFloat, y: CGFloat, attributes: [NSAttributedString.Key : Any], constrainedToSize: CGSize, anchor: CGPoint, angleRadians: CGFloat) {
            let line = formattedLabel.split(separator: "\n")
            print("formatted label \(formattedLabel) after split \(String(line[0]))")
            scrollDate = String(line[0])
            print("scroll date \(scrollDate)")
//            dateChangeLabel.text = String(line[0])
        }
    }
}


extension UINavigationController {
    var canHideBottomForNextPush:Bool {
        guard #available(iOS 14, *) else {
            return true
        }
        return viewControllers.count == 1
    }
}
