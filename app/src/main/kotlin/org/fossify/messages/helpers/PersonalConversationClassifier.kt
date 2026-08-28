package org.fossify.messages.helpers
import android.content.Context
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import org.fossify.messages.models.Conversation
class PersonalConversationClassifier(context:Context){private val c=context.applicationContext;private var numbers:Set<String> = emptySet();fun ensureLoaded(onLoaded:(()->Unit)?=null){onLoaded?.invoke()};fun isPersonal(conversation:Conversation,isBank:Boolean):Boolean{if(conversation.isGroupConversation||isBank)return false;val n=canonical(conversation.phoneNumber);return n?.let{it.startsWith("09")&&it.length==11||it in numbers}?:false};private fun canonical(number:String?):String?{if(number.isNullOrBlank())return null;val d=PhoneNumberUtils.normalizeNumber(PhoneNumberUtils.replaceUnicodeDigits(number)).removePrefix("+");return when{d.startsWith("0098")&&d.length==14->"0"+d.substring(4);d.startsWith("98")&&d.length==12->"0"+d.substring(2);d.length==10&&d.startsWith("9")->"0$d";else->d}}}
