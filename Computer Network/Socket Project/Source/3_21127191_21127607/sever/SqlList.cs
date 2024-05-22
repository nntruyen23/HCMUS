using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace sever
{
    internal class SqlList
    {
        public string name { get; set; }
        public string filename { get; set; }
        
        public byte[] image { get; set; }
        public string type { get; set; }
        public string detail { get; set; }
        public string cost { get; set; }
        public string amount { get; set; }
       
        SqlList(string name, byte[] image, string filename, string type, string detail, string cost, string amount)
        {
            this.name = name;
            this.image = image;
            this.filename = filename;   
            this.type = type;
            this.detail = detail;
            this.cost = cost;
            this.amount = amount;
           
        }
    }
}
