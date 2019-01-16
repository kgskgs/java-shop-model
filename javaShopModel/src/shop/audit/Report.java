/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shop.audit;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import shop.infrastructure.MySqlRepository;
import shop.models.BoughtProduct;
import shop.models.Employee;
import shop.models.Product;
import shop.models.Receipt;

/**
 *
 * @author Lyuboslav
 */
public class Report{
    private final Connection sqlConnection;
    private final BlockingQueue<String> sqlQueue;
    
    public Report(Connection sqlConnection, BlockingQueue<String> sqlQueue)
    {
        this.sqlConnection =   sqlConnection;
        this.sqlQueue = sqlQueue;
    }     
    
    public ArrayList<String> WriteReportReceipts(Employee employee, Date startDate, Date endDate) throws SQLException, ParseException
    {
        ArrayList<String> result = new ArrayList<>();
        
        MySqlRepository<Receipt> receiptRepo = new MySqlRepository<>(Receipt.class,sqlConnection,null);
        
        ArrayList<Receipt> receipts = receiptRepo.GetAll(0, receiptRepo.Count(), true);
        ArrayList<Receipt> thisEmployeeReceipts = new ArrayList<>();
        
        DateFormat format;
        format = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss", Locale.ENGLISH);
        
        for (Receipt receipt : receipts){
            Date receiptDate = (Date) format.parse(receipt.buyDate);
            if (receipt.employeeId == employee.employeeId && receiptDate.before(endDate) && receiptDate.after(startDate)) {
                thisEmployeeReceipts.add(receipt);
            }
        }
        
        MySqlRepository<BoughtProduct> boughtProductsRepo = new MySqlRepository<>(BoughtProduct.class,sqlConnection,null); 
        MySqlRepository<Product> productsRepo = new MySqlRepository<>(Product.class,sqlConnection,null); 
        
        ArrayList<BoughtProduct> boughtProducts = boughtProductsRepo.GetAll(0, boughtProductsRepo.Count(), true);
       
        
        for (Receipt receipt : thisEmployeeReceipts){
            result.add("Receipt " + receipt.receiptId + " " + receipt.buyDate);
            for(BoughtProduct boughtProduct : boughtProducts){
                if (boughtProduct.receiptId == receipt.receiptId) {
                    Product product = productsRepo.Get(boughtProduct.productId);
                    result.add(product.productName + " " + product.price);
                }
            }            
        }
        
        
        return result;
    }
    
    
}
