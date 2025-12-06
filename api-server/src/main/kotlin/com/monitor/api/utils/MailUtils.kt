package com.monitor.api.utils


import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component

@Component
class MailUtils(
    private val mailSender: JavaMailSender
) {

    fun sendSimpleMail(
        to: String,
        subject: String,
        text: String,
        from: String = "no-reply@insung.com"
    ) {
        val message = SimpleMailMessage().apply {
            setTo(to)
            setSubject(subject)
            setText(text)
            setFrom(from)
        }

        mailSender.send(message)
    }
}