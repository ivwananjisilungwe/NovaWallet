/// Fee estimate mirroring backend `FeeEstimateResponse`.
class FeeEstimate {
  const FeeEstimate({
    required this.transactionType,
    required this.amount,
    this.percentageFee,
    this.flatFee,
    this.minFee,
    this.maxFee,
    required this.totalFee,
  });

  final String transactionType;
  final String amount;
  final String? percentageFee;
  final String? flatFee;
  final String? minFee;
  final String? maxFee;
  final String totalFee;

  factory FeeEstimate.fromJson(Map<String, dynamic> json) => FeeEstimate(
        transactionType: (json['transactionType'] ?? '').toString(),
        amount: (json['amount'] ?? '0').toString(),
        percentageFee: json['percentageFee']?.toString(),
        flatFee: json['flatFee']?.toString(),
        minFee: json['minFee']?.toString(),
        maxFee: json['maxFee']?.toString(),
        totalFee: (json['totalFee'] ?? '0').toString(),
      );
}
