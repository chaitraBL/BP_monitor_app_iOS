//
//  LogTableViewCell.swift
//  BP monitor app
//
//  Created by fueb on 28/06/22.
//  Copyright © 2022 fueb. All rights reserved.
//

import UIKit

class LogTableViewCell: UITableViewCell {
    
    
    @IBOutlet weak var date: UILabel!
    
    @IBOutlet weak var time: UILabel!
    @IBOutlet weak var sys: UILabel!
    @IBOutlet weak var dia: UILabel!
    @IBOutlet weak var rate: UILabel!
    
    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)

        // Configure the view for the selected state
    }

}
