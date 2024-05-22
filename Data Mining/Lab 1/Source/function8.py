import csv
import argparse

def is_numeric_float(value):
    try:
        float(value)
        return True
    except ValueError:
        return False

def cal_between_two_col(col1, col2, operator, input_file, output_file):
    with open(input_file) as csv_file:
        csv_reader = csv.reader(csv_file, delimiter=',')
        header = next(csv_reader)

        column_types = []

        data = []
        for _ in range(len(header)):
            data.append([])

        for row in csv_reader:
            for i, value in enumerate(row):
                data[i].append(value)
        for i in range(len(data)):
            b = False
            for j in data[i]:
                if j.isnumeric() or is_numeric_float(j):
                    column_types.append('numeric')
                    b = True
                    break
                else:
                    if j.strip() == '':
                        continue
            if b == False:
                column_types.append('categorical')
    
    for i, value in enumerate(column_types):
        if i == col1 or col2 == i: 
            if value != 'numeric':
                print('Column is not numeric!')
                return
    
    header.append('new_column')
    data.append([])
    lenH = len(header) - 1
    if operator == "+":
        data[lenH] = [float(x) + float(y) if x != '' and y != '' else 0 for x, y in zip(data[col1], data[col2])]
    elif operator == "-":
        data[lenH] = [float(x) - float(y) if x != '' and y != '' else 0 for x, y in zip(data[col1], data[col2])]
    elif operator == "*":
        data[lenH] = [float(x) * float(y) if x != '' and y != '' else 0 for x, y in zip(data[col1], data[col2])]
    elif operator == "/":
        data[lenH] = [float(x) / float(y) if x != '' and y != '' and y != 0 else 0 for x, y in zip(data[col1], data[col2])]
    
    with open(output_file, 'w', newline='') as csvfile:
        csv_writer = csv.writer(csvfile)
        csv_writer.writerow(header)
        for i in range(len(data[0])):
            row = [data[j][i] for j in range(len(header))]
            csv_writer.writerow(row)

# Create an argument parser
parser = argparse.ArgumentParser(description='Perform calculations between two columns in a CSV file.')

# Add arguments for the column indices, operator, input file, and output file
parser.add_argument('input_file', type=str, help='Name of the input CSV file')
parser.add_argument('--col1', type=int, help='Index of the first column')
parser.add_argument('--col2', type=int, help='Index of the second column')
parser.add_argument('--operator', choices=['+', '-', '*', '/'], help='Operator for the calculation: "+", "-", "*", or "/"')
parser.add_argument('--output_file', type=str, help='Name of the output CSV file')

# Parse the command-line arguments
args = parser.parse_args()

# Call the cal_between_two_col function with the provided arguments
cal_between_two_col(args.col1, args.col2, args.operator, args.input_file, args.output_file)