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
using System.Globalization;
namespace client
{
    
    public partial class Booking : Form
    {
        string UserName;
        public Booking(string username)
        {
            InitializeComponent();
            UserName = username;
        }

        private void label2_Click(object sender, EventArgs e)
        {
            txtName.Clear();
            txtType.Clear();
            txtNote.Clear();
        }
        bool checkDaTa(string name, string type)
        {
            SqlConnection conn = new SqlConnection(@"Data Source=LAPTOP-DF1LB0CM\NHATTRUYEN;Initial Catalog=Account;Integrated Security=True");
            try
            {
                if (conn.State == ConnectionState.Closed)
                {
                    conn.Open();
                }
                string sql = "select *from phong where Ten='" + name + "' and LoaiPhong='" + type + "'";
                SqlCommand cmd = new SqlCommand(sql, conn);
                SqlDataReader dta = cmd.ExecuteReader();
                if (dta.Read() == true)
                {
                    conn.Close();
                    return false;
                }
                else
                {
                    conn.Close();
                    return true;
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show("Connection Eror");
                return false;
            }
        }
        private void button1_Click(object sender, EventArgs e)
        {
            if (!checkDaTa(txtName.Text, txtType.Text))
            {
                MessageBox.Show("Tên khách sạn hoặc loại phòng không hợp lệ vui lòng kiểm tra lại!" + txtName.Text + txtType.Text);

            }
            else
            {
                DateTime dt1 = dateTimePicker1.Value;
                DateTime dt2 = dateTimePicker2.Value;
                int result = DateTime.Compare(dt1, dt2);
                if (result > 0)
                {
                    MessageBox.Show("Ngày đến và ngày đi không hợp lệ vui lòng kiểm tra lại!");
                }
                else
                {
                    int i = 0;
                    while (i < txtAmount.Text.Length && ('0' <= txtAmount.Text[i] && '9' >= txtAmount.Text[i]))
                    {
                        i++;
                    }
                    if (i < txtAmount.Text.Length)
                    {
                        MessageBox.Show("Số lượng không hợp lệ vui lòng kiểm tra lại!");
                    }
                    else
                    {
                        float total = 0;
                        int amount = Int32.Parse(txtAmount.Text);
                        CalculateBill(txtName.Text, txtType.Text, dt1, dt2, amount);
                    }
                }
            }

        }
        void CalculateBill(string name, string type, DateTime dt1, DateTime dt2, int amount)
        {
            float cost = 0;
            //string gia = "0";
            TimeSpan difference = dt2 - dt1;
            int day = difference.Days+1;
            SqlConnection conn = new SqlConnection(@"Data Source=LAPTOP-DF1LB0CM\NHATTRUYEN;Initial Catalog=Account;Integrated Security=True");
            try
            {
                if (conn.State == ConnectionState.Closed)
                {
                    conn.Open();
                }
                using (DataTable dt = new DataTable("phong"))
                {
                    SqlDataAdapter adapter = new SqlDataAdapter("select *from phong", conn);
                    adapter.Fill(dt);
                    if (dt != null)
                    {
                        foreach (DataRow row in dt.Rows)
                            if (row["Ten"].ToString() == name && row["LoaiPhong"].ToString() == type)
                            {
                                string gia = row["Gia"].ToString();
                                cost = float.Parse(gia, CultureInfo.InvariantCulture.NumberFormat);
                                break;
                            }
                    }
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show("Connection Eror");
            }

            conn.Close();
            
            this.Hide();
            new Bill(UserName,txtName.Text, txtType.Text, dt1, dt2, cost, cost * amount*day, amount).Show();
        }
        private void label3_Click(object sender, EventArgs e)
        {
            this.Hide();
        }

        private void label9_Click(object sender, EventArgs e)
        {

        }
    }
}
