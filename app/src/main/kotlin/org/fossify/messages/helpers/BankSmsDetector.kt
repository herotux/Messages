package org.fossify.messages.helpers
object BankSmsDetector {
 data class Detection(val bank:IranianBankRegistry.BankInfo)
 fun detect(sender:String?,message:String?):Detection?{
  val s=normalize(sender);val m=normalize(message);val b=when{
   s=="+982122333333"||s.contains("sepah")||s.contains("sepa")||s.contains("627648")||m.contains("بانک سپه")->IranianBankRegistry.byId("sepah")
   s.contains("170019")||s.contains("melli")||m.contains("بانک ملی")->IranianBankRegistry.byId("melli")
   s.contains("200010")||s.contains("tejarat")||m.contains("بانک تجارت")->IranianBankRegistry.byId("tejarat")
   else->null
  }
  return b?.let(::Detection)
 }
 private fun normalize(v:String?)=v.orEmpty().replace('ي','ی').replace('ك','ک').replace('۰','0').replace('۱','1').replace('۲','2').replace('۳','3').replace('۴','4').replace('۵','5').replace('۶','6').replace('۷','7').replace('۸','8').replace('۹','9').lowercase().replace(" ","")
}
