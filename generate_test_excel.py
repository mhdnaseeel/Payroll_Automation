import pandas as pd

# Create dummy employee data matching the columns expected by Java POI
# MemberID, Name, UAN, IP, Bank, IFSC
data = {
    'MemberID': ['1001', '1002'],
    'Name': ['Alice Test', 'Bob Sample'],
    'UAN': ['123456789012', '987654321098'],
    'IP': ['IP123', 'IP456'],
    'Bank': ['1122334455', '5566778899'],
    'IFSC': ['SBIN0001234', 'HDFC0005678']
}

df = pd.DataFrame(data)

# Save to Excel
df.to_excel('test_employees.xlsx', index=False)
print("Created test_employees.xlsx")
