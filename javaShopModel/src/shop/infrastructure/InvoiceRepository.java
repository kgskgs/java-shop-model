/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shop.infrastructure;

import java.sql.Connection;
import java.sql.SQLException;
import shop.infrastructure.interfaces.Repository;
import shop.models.Invoice;

/**
 *
 * @author Lyuboslav
 */
public class InvoiceRepository extends Repository<Invoice>{

    public InvoiceRepository(Connection sqlConnection)
    {
        super(Invoice.class, sqlConnection);    
    }

    @Override
    public Invoice[] GetAll(int page, int count) {
        return getAllAndBind(page, count);
    }

    @Override
    public Invoice Get(int key) {
        return getAndBind(key);
    }

    @Override
    public void Insert(Invoice model) {
        executeInsert(model);
    }

    @Override
    public void Delete(Invoice model) {
        executeDelete(model.invoiceId);
    }
}
