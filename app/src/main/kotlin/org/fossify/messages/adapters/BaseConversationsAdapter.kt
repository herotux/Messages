    override fun getItemKeyPosition(key: Int) = currentList.indexOfFirst { it.hashCode() == key }
    override fun onActionModeCreated() {}
    override fun onActionModeDestroyed() {}
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = createViewHolder(ItemConversationBinding.inflate(layoutInflater, parent, false).root)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) { val conversation = getItem(position); holder.bindView(conversation, allowSingleClick = true, allowLongClick = true) { itemView, _ -> setupView(itemView, conversation) }; bindViewHolder(holder) }
    override fun getItemId(position: Int) = getItem(position).threadId
    override fun onViewRecycled(holder: ViewHolder) { super.onViewRecycled(holder); if (!activity.isDestroyed && !activity.isFinishing) Glide.with(activity).clear(ItemConversationBinding.bind(holder.itemView).conversationImage) }
    private fun fetchDrafts(drafts: HashMap<Long, String>) { drafts.clear(); for ((threadId, draft) in activity.getAllDrafts()) drafts[threadId] = draft }

    private fun setupView(view: View, conversation: Conversation) {
        ItemConversationBinding.bind(view).apply {
            root.setupViewBackground(activity)
            val smsDraft = drafts[conversation.threadId]
            draftIndicator.beVisibleIf(!smsDraft.isNullOrEmpty()); draftIndicator.setTextColor(properPrimaryColor)
            pinIndicator.beVisibleIf(activity.config.pinnedConversations.contains(conversation.threadId.toString())); pinIndicator.applyColorFilter(textColor)
            conversationFrame.isSelected = selectedKeys.contains(conversation.hashCode())
            conversationAddress.text = conversation.title; conversationAddress.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 1.2f)
            conversationBodyShort.text = smsDraft ?: conversation.snippet; conversationBodyShort.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 0.9f)
            conversationDate.text = if (activity.config.usePersianCalendar) PersianDateHelper.toPersianDigits(PersianDateHelper.formatMonthName(conversation.date * 1000L)) else (conversation.date * 1000L).formatDateOrTime(context = activity, hideTimeOnOtherDays = true, showCurrentYear = false)
            conversationDate.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize * 0.8f)
            val isUnread = !conversation.read
            conversationBodyShort.alpha = if (isUnread) 1f else 0.7f
            val style = if (isUnread) { if (conversation.isScheduled) Typeface.BOLD_ITALIC else Typeface.BOLD } else { if (conversation.isScheduled) Typeface.ITALIC else Typeface.NORMAL }
            val customTypeface = FontHelper.getTypeface(activity); conversationAddress.setTypeface(customTypeface, style); conversationBodyShort.setTypeface(customTypeface, style); conversationDate.setTypeface(customTypeface, style)
            arrayListOf(conversationAddress, conversationBodyShort, conversationDate).forEach { it.setTextColor(textColor) }
            setupBadgeCount(unreadCountBadge, isUnread, conversation.unreadCount)

            val placeholder = if (conversation.isGroupConversation) SimpleContactsHelper(activity).getColoredGroupIcon(conversation.title) else null
            val confirmedBank = if (!conversation.isGroupConversation) BankConversationVerificationStore.getConfirmedBank(activity, conversation.threadId) else null
            val bankDetection = if (!conversation.isGroupConversation && confirmedBank == null) BankSmsDetector.detect(conversation.phoneNumber, conversation.snippet) else null
            val bank = confirmedBank ?: bankDetection?.bank
            val bankLogoRes = bank?.let { IranianBankLogoResolver.resolve(activity, it) }
            val senderLogoRes = if (!conversation.isGroupConversation && bankLogoRes == null) IranianSenderIconResolver.resolve(activity, conversation.phoneNumber) else null
            val logoRes = bankLogoRes ?: senderLogoRes
            if (logoRes != null) {
                Glide.with(activity).clear(conversationImage)
                if (!IranianBankLogoImageHelper.setBankLogo(conversationImage, logoRes)) {
                    SimpleContactsHelper(activity).loadContactImage(conversation.photoUri, conversationImage, conversation.title, placeholderImage = placeholder)
                }
            } else if (conversation.isGroupConversation) {
                SimpleContactsHelper(activity).loadContactImage(conversation.photoUri, conversationImage, conversation.title, placeholderImage = placeholder)
            } else {
                SimpleContactsHelper(activity).loadContactImage(conversation.photoUri, conversationImage, conversation.title)
            }
        }
    }
