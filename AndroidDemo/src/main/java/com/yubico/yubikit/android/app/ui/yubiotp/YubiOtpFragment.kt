/*
 * Copyright (C) 2022 Yubico.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yubico.yubikit.android.app.ui.yubiotp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.yubico.yubikit.android.app.MainViewModel
import com.yubico.yubikit.android.app.R
import com.yubico.yubikit.android.app.databinding.FragmentYubiotpOtpBinding
import com.yubico.yubikit.android.ui.OtpActivity
import com.yubico.yubikit.core.otp.Modhex
import com.yubico.yubikit.core.util.RandomUtils
import com.yubico.yubikit.yubiotp.Slot
import com.yubico.yubikit.yubiotp.YubiOtpSlotConfiguration
import org.bouncycastle.util.encoders.Hex

class YubiOtpFragment : Fragment() {
    class OtpContract : ActivityResultContract<Unit, String?>() {
        override fun createIntent(context: Context, input: Unit): Intent = Intent(context, OtpActivity::class.java)

        override fun parseResult(resultCode: Int, intent: Intent?): String? {
            return intent?.getStringExtra(OtpActivity.EXTRA_OTP)
        }
    }

    private val requestOtp = registerForActivityResult(OtpContract()) {
        activityViewModel.setYubiKeyListenerEnabled(true)
        viewModel.postResult(Result.success(when (it) {
            null -> "Cancelled by user"
            else -> "Read OTP: $it"
        }))
    }

    private val activityViewModel: MainViewModel by activityViewModels()
    private val viewModel: OtpViewModel by activityViewModels()
    private lateinit var binding: FragmentYubiotpOtpBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        binding = FragmentYubiotpOtpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.textLayoutPublicId.setEndIconOnClickListener {
            binding.editTextPublicId.setText(Modhex.encode(RandomUtils.getRandomBytes(6)))
        }
        binding.editTextPublicId.setText(Modhex.encode(RandomUtils.getRandomBytes(6)))

        binding.textLayoutPrivateId.setEndIconOnClickListener {
            binding.editTextPrivateId.setText(String(Hex.encode(RandomUtils.getRandomBytes(6))))
        }
        binding.editTextPrivateId.setText(String(Hex.encode(RandomUtils.getRandomBytes(6))))

        binding.textLayoutKey.setEndIconOnClickListener {
            binding.editTextKey.setText(String(Hex.encode(RandomUtils.getRandomBytes(16))))
        }
        binding.editTextKey.setText(String(Hex.encode(RandomUtils.getRandomBytes(16))))

        binding.btnSave.setOnClickListener {
            try {
                val publicId = Modhex.decode(binding.editTextPublicId.text.toString())
                val privateId = Hex.decode(binding.editTextPrivateId.text.toString())
                val key = Hex.decode(binding.editTextKey.text.toString())
                val slot = when (binding.slotRadio.checkedRadioButtonId) {
                    R.id.radio_slot_1 -> Slot.ONE
                    R.id.radio_slot_2 -> Slot.TWO
                    else -> throw IllegalStateException("No slot selected")
                }

                viewModel.pendingAction.value = {
                    putConfiguration(slot, YubiOtpSlotConfiguration(publicId, privateId, key), null, null)
                    "Slot $slot programmed"
                }
            } catch (e: Exception) {
                viewModel.postResult(Result.failure(e))
            }
        }

        binding.btnRequestOtp.setOnClickListener {
            activityViewModel.setYubiKeyListenerEnabled(false)
            requestOtp.launch(null)
        }
    }
}