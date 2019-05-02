import java.sql.Date;

public class MainSales extends Sales {
    public int boID;

    public MainSales(int id, Date date, String region, String product, int qty, double cost, double total, int boID) {
        super(id, date, region, product, qty, cost, total);
        this.boID = boID;
    }

}
