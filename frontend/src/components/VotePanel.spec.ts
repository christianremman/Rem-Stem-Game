import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import VotePanel from './VotePanel.vue'

describe('VotePanel', () => {
  it('emits DONE, REFUSED, and BOO votes from the controls', async () => {
    const wrapper = mount(VotePanel)
    const buttons = wrapper.findAll('button')

    await buttons[0].trigger('click')
    await buttons[1].trigger('click')
    await buttons[2].trigger('click')

    expect(wrapper.emitted('vote')).toEqual([[ 'DONE' ], [ 'REFUSED' ], [ 'BOO' ]])
  })
})
