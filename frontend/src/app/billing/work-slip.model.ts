export interface WorkSlip {
    id?: string;
    slipNumber: string;
    category: 'ISSUE' | 'RECEIPT' | 'QC';
    entryDate: string; // YYYY-MM-DD
    imagePath?: string;
    // Common
    truckNumber: string;
    commodity: string;

    // Issue
    issueTotalBags?: number;

    // Receipt (Height-wise)
    receiptTotalBags?: number; // Must match sum of below
    bagsUpTo10?: number;
    bags11To16?: number;
    bags17To20?: number;
    bagsAbove20?: number;

    // QC
    labourCount?: number;

    shedDetails?: string;
}
