import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.apache.commons.lang3.SerializationUtils;


public class BranchOffice {
    private java.sql.Connection connection;
    private String url;
    private ArrayList<Sales> sales;
    private ArrayList<Sales> currentSales;
    private String region;
    private  String queue;

    private static String[] columnNames={"date","region","product","qty","cost","total"};


    public BranchOffice(String url,String region,String queue) {
        this.sales =new ArrayList<Sales>();
        this.currentSales =new ArrayList<Sales>();
        this.url=url;
        this.region=region;
        this.queue=queue;

    }

    public void addSale(Sales sale){
        this.sales.add(sale);
    }

    public void connectToDb(){
        try {
            Class.forName ("org.h2.Driver");
            this.connection = DriverManager.
                    getConnection("jdbc:h2:~/"+url, "sa", "");
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void init(){
        connectToDb();
        JFrame frame = new JFrame("Sales Management");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JButton jButton1=new JButton("show data");
        jButton1.setBounds(120,130,150,20);
        jButton1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fetch_data();
                show_data();
            }
        });

        JButton jButton2=new JButton("add data");
        jButton2.setBounds(120,130,150,20);
        jButton2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                add_data();
            }
        });

        JButton jButton3=new JButton("Send to main Server");
        jButton3.setBounds(120,130,150,20);
        jButton3.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                send_data();
            }
        });

        frame.add(jButton1);
        frame.add(jButton2);
        frame.add(jButton3);
        frame.setLayout(new FlowLayout());
        frame.setVisible(true);
        frame.setSize(500, 400);
    }

    public  void  fetch_data()  {
        try {
            String req = "select * from sales";
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(req);
            this.sales.clear();
            while (resultSet.next()) {
                int id=resultSet.getInt(1);
                Date date =resultSet.getDate(2);
                String region=resultSet.getString(3);
                String product=resultSet.getString(4);
                int qty=resultSet.getInt(5);
                double cost=resultSet.getDouble(6);
                double total=resultSet.getDouble(7);
                Sales sale=new Sales(id,date,region,product,qty,cost,total);
                this.sales.add(sale);
            }

        }catch(SQLException e){
            e.printStackTrace();
        }
    }

    public void show_data(){
        JFrame frame = new JFrame("Database Search Result");
        //frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
//TableModel tm = new TableModel();
        DefaultTableModel model = new DefaultTableModel();
        model.setColumnIdentifiers(columnNames);
        JTable table = new JTable();
        table.setModel(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        //table.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        String date= "";
        String region= "";
        String product= "";
        String qty= "";
        String cost="";
        String total="";
        for (int i=0; i<sales.size(); i++){
            date=sales.get(i).getDate().toString();
            region=sales.get(i).getRegion();
            product=sales.get(i).getProduct();
            Integer qt=new Integer(sales.get(i).getQty());
            qty = qt.toString();
            Double cst=new Double(sales.get(i).getCost());
            cost=cst.toString();
            Double tot=new Double(sales.get(i).getTotal());
            total=tot.toString();
            model.addRow(new Object[]{date,region,product, qty,cost,total});
        }
        frame.add(scroll);
        frame.setVisible(true);
        frame.setSize(400,300);


    }

    public void add_data(){
        JFrame frame = new JFrame("Adding sales");

        JLabel labelDate=new JLabel("Date");
        final JTextField dateField=new JTextField(5);

        JLabel labelProduct=new JLabel("Product");
        final JTextField productField=new JTextField(5);

        JLabel labelQty=new JLabel("Quantity");
        final JTextField qtyField=new JTextField(5);

        JLabel labelCost=new JLabel("Cost");
        final JTextField costField=new JTextField(5);

        JButton submitButoon=new JButton("Submit");
        submitButoon.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String date=dateField.getText();
                String product=productField.getText();
                int qty= Integer.parseInt(qtyField.getText());
                double cost=Double.parseDouble(costField.getText());
                try {
                    PreparedStatement statement = connection.prepareStatement
                            ("Insert into sales(date,region,product,qty,cost,total) values(?,?,?,?,?,?)");
                    statement.setString(1,date);
                    statement.setString(2,region);
                    statement.setString(3,product);
                    statement.setInt(4,qty);
                    statement.setDouble(5,cost);
                    statement.setDouble(6,cost*qty);
                    int affected=statement.executeUpdate();
                    if(affected!=0){
                        Sales sale =new Sales();
                        ResultSet generatedKeys=statement.getGeneratedKeys();
                        if(generatedKeys.next()){
                            sale.setId(generatedKeys.getInt(1));
                            sale.setCost(cost);
                            Date dateSql = Date.valueOf(date);
                            sale.setDate(dateSql);
                            sale.setProduct(product);
                            sale.setQty(qty);
                            sale.setRegion(region);
                            sale.setTotal(cost*qty);
                            currentSales.add(sale);
                        }
                    }
                }catch(SQLException ex){
                    ex.printStackTrace();
                }
            }
        });

        frame.add(labelDate);
        frame.add(dateField);
        frame.add(labelProduct);
        frame.add(productField);
        frame.add(labelQty);
        frame.add(qtyField);
        frame.add(labelCost);
        frame.add(costField);
        frame.add(submitButoon);

        frame.setLayout(new FlowLayout());
        frame.setVisible(true);
        frame.setSize(500, 400);

    }





    public void send_data(){
        fetch_data();
        ConnectionFactory factory=new ConnectionFactory();
        factory.setHost("localhost");
        try{
            Connection connection =factory.newConnection();
            Channel channel=connection.createChannel();
            channel.queueDeclare(queue,false,false,false,null);
            byte[] data = SerializationUtils.serialize(currentSales);
            channel.basicPublish("",queue,null,data);

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static void  main(String args[]){
        BranchOffice bo1=new BranchOffice("bo1","west","queue_1");
        bo1.init();
        BranchOffice bo2=new BranchOffice("bo2","east","queue_2");
        bo2.init();
    }

}
