//
//  ReadingsController.swift
//  BP monitor app
//
//  Created by fueb on 27/06/22.
//  Copyright © 2022 fueb. All rights reserved.
//

import UIKit

class ReadingsController: UIViewController {

    @IBOutlet weak var imgView: UIImageView!
    override func viewDidLoad() {
        super.viewDidLoad()

        containerView.frame = CGRect(x: view.frame.width/2 - 100, y: view.frame.height/2 - 100, width: 200, height: 200)
        view.addSubview(containerView)
        
        drawArc()
    }
    
    let containerView: UIView = {
        let view = UIView()
        view.backgroundColor = .clear
        return view
    }()
    
    private func drawArc() {
        let renderer = UIGraphicsImageRenderer(size: CGSize(width: 280, height: 250))
        
        let img = renderer.image { ctx in
            let rect = CGRect(x: 5, y: 5, width: 100, height: 100)
            
            // 6
//            ctx.cgContext.setFillColor(UIColor.blue.cgColor)
            ctx.cgContext.setStrokeColor(UIColor.black.cgColor)
            ctx.cgContext.setLineWidth(5)
            
            ctx.cgContext.addEllipse(in: rect)
            ctx.cgContext.drawPath(using: .fillStroke)
        }
        
        imgView.image = img
    }

}
