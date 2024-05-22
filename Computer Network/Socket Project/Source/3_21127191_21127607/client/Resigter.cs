using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;
using System.Net;
using System.Net.Sockets;
using System.Runtime.Serialization.Formatters.Binary;
namespace client
{
    public partial class Resigter : Form
    {
        Socket client;
        public Resigter(Socket s)
        {
            InitializeComponent();
            //CheckForIllegalCrossThreadCalls = false;
            client = s;
            txtUserName.Focus();
            
        }
        bool checkAcc(string data)
        {
            if (data == null || data.Length == 0) return false;
            for(int i = 0; i < data.Length; i++)
            {
                if (data[i] == ' ') return false;
            }
            return true;
        }
        void Send()
        {
            if (!checkAcc(txtUserName.Text))
            {
                MessageBox.Show("UserName unavailable!");
            }
            else if(!checkAcc(txtPassWord.Text))
            {
                MessageBox.Show("PassWord unavailable!");
            }
            else if (txtCPassWord.Text != txtPassWord.Text)
            {
                MessageBox.Show("ConformPassWord incorrect!");
            }
            else if (!checkAcc(txtBankAcc.Text))
            {
                MessageBox.Show("Bank Account unavailable!");
            }
            else
            {
                Send("1 " + txtUserName.Text + " " + txtPassWord.Text + " " + txtBankAcc.Text);
            }
        }
        private void label1_Click(object sender, EventArgs e)
        {

        }

        private void pictureBox1_Click(object sender, EventArgs e)
        {

        }

        private void pictureBox3_Click(object sender, EventArgs e)
        {

        }

        private void Resigter_Load(object sender, EventArgs e)
        {

        }

        private void label5_Click(object sender, EventArgs e)
        {

        }

        private void label6_Click(object sender, EventArgs e)
        {

        }

        private void textBox1_TextChanged(object sender, EventArgs e)
        {

        }

        private void panel5_Paint(object sender, PaintEventArgs e)
        {

        }

        private void txtUserName_TextChanged(object sender, EventArgs e)
        {

        }

        private void label9_Click(object sender, EventArgs e)
        {

        }

        private void button1_Click(object sender, EventArgs e)
        {
            Send();  
            Receive();
        }

        private void label2_Click(object sender, EventArgs e)
        {
            txtUserName.Clear();
            txtPassWord.Clear();
            txtCPassWord.Clear();
            txtBankAcc.Clear();
            txtUserName.Focus();
        }

        private void textBox2_TextChanged(object sender, EventArgs e)
        {

        }

        private void label3_Click(object sender, EventArgs e)
        {
            Close();
        }

        private void label4_Click(object sender, EventArgs e)
        {
            new SignIn().Show();
            this.Hide();

        }

        private void txtBankAcc_TextChanged(object sender, EventArgs e)
        {

        }

        private void Resigter_FormClosed(object sender, FormClosedEventArgs e)
        {
            Close();
        }

        void close()
        {
            client.Close();
        }
        public void Send(String data)
        {

            if (data != String.Empty)
            {
                client.Send(Serialize(data));
            }
        }
        public void Receive()
        {
            try
            {
                    byte[] data = new byte[1024 * 5000];

                    client.Receive(data);

                    string message = (string)Deserialize(data);

                    if (message == null)
                    {
                        MessageBox.Show("Resigter unsuccess! Resigter again.");
                    }
                    else if (message == "10")
                    {
                        MessageBox.Show("Resigter success.");
                        txtUserName.Clear();
                        txtPassWord.Clear();
                        txtCPassWord.Clear();
                        txtBankAcc.Clear();
                        this.Hide();
                      
                    }
                    else if (message == "11")
                    {
                        MessageBox.Show("UserName unavailable!");
                    }
                    else if (message == "12")
                    {
                        MessageBox.Show("PassWord unavailable!");
                    }
                    else if (message == "13")
                    {
                        MessageBox.Show("Bank Account unavailable!");
                    }
                    else if (message == "14")
                    {
                        MessageBox.Show("Username exists!");
                    }
            }
            catch
            {
                close();
            }
        }

        byte[] Serialize(object obj)
        {
            MemoryStream stream = new MemoryStream();
            BinaryFormatter formatter = new BinaryFormatter();

            formatter.Serialize(stream, obj);

            return stream.ToArray();

        }
        //gom manh
        object Deserialize(byte[] data)
        {
            MemoryStream stream = new MemoryStream(data);
            BinaryFormatter formatter = new BinaryFormatter();

            return formatter.Deserialize(stream);
        }

    }
}

