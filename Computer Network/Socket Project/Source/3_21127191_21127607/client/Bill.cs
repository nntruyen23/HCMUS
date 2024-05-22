using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;
using System.Data;
using System.Data.SqlClient;

namespace client
{
    public partial class Bill : Form
    {
        public Bill(string username,string name, string type, DateTime dt1, DateTime dt2, float cost, float total, int amount)
        {
            InitializeComponent();
            txtName.Text = name;
            txtType.Text = type;
            dateTimePicker1.Value = dt1;
            dateTimePicker2.Value = dt2;
            txtCost.Text = cost.ToString();
            txtAmount.Text = amount.ToString();
            txtTotal.Text = total.ToString();
            //insertData(username, name, type, dt1, dt2, cost, total, amount);
        }
        void insertData(string username, string name, string type, DateTime dt1, DateTime dt2, float cost, float total, int amount)
        {
            SqlConnection conn = new SqlConnection(@"Data Source=LAPTOP-DF1LB0CM\NHATTRUYEN;Initial Catalog=Account;Integrated Security=True");
            string format = "YYYY-MM-DD";
            try
            {
                conn.Open();
                string sql = "Insert into datphong values ('" + username + "','" + name + "','" + type + "','" + dt1 + "','" + dt2 + "','" + amount + "','" + cost+ "','" + total + "')";
                SqlCommand cmd = new SqlCommand(sql, conn);
                cmd.ExecuteNonQuery();
                conn.Close();
            }
            catch (Exception ex)
            {
                MessageBox.Show("Connection Eror");
            }
            Close();
        }
        private void label3_Click(object sender, EventArgs e)
        {
            this.Hide();
        }
    }
}
