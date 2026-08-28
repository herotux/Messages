package org.fossify.messages.helpers
object IranianBankRegistry {
 data class BankInfo(val id:String,val displayName:String,val logoResourceName:String?=null)
 val banks=listOf(
  BankInfo("sepah","بانک سپه","bank_sepah"),
  BankInfo("melli","بانک ملی","bank_melli"),
  BankInfo("tejarat","بانک تجارت","bank_tejarat"),
  BankInfo("mellat","بانک ملت","bank_mellat"),
  BankInfo("shahr","بانک شهر","bank_shahr"),
  BankInfo("parsian","بانک پارسیان","bank_parsian"),
  BankInfo("pasargad","بانک پاسارگاد","bank_pasargad"),
  BankInfo("post","پست بانک","bank_post"),
  BankInfo("refah","بانک رفاه","bank_refah"),
  BankInfo("resalat","بانک قرض‌الحسنه رسالت","bank_resalat")
 )
 fun byId(id:String)=banks.firstOrNull{it.id==id}
}
