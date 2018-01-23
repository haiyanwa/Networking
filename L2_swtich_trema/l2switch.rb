#
#layer-2 switch using trema#
#
require 'fdb'
require 'ipaddr'

class L2Switch < Controller

  def start
    @fdb = FDB.new
  end

  def switch_ready(datapath_id)
    action = SendOutPort.new(port_number: OFPP_CONTROLLER, max_len: OFPCML_NO_BUFFER)
    ins = ApplyAction.new(actions: [action])
    send_flow_mod_add(datapath_id,
                      priority: OFP_LOW_PRIORITY,
                      buffer_id: OFP_NO_BUFFER,
                      flags: OFPFF_SEND_FLOW_REM,
                      instructions: [ins]
    )
  end

  def packet_in(datapath_id, message)
    macsa = message.eth_src
    macda = message.eth_dst
    ipsa = message.ipv4_src
    ipda = message.ipv4_dst

    #host1ip = IPAddr.new("192.168.0.1");
    if(ipsa)
	### Change IP addr to String
        sip = ipsa.to_s()
        dip = ipda.to_s()
      #if (ipsa.eql? host1ip)
      if ((sip.eql? "192.168.0.1") || (dip.eql? "192.168.0.1"))
    	puts 'received a packet_in'
    	info "datapath_id: #{ datapath_id.to_hex }"
    	info "transaction_id: #{ message.transaction_id.to_hex }"
    	info "buffer_id: #{ message.buffer_id.to_hex }"
    	info "total_len: #{ message.total_len }"
    	info "reason: #{ message.reason.to_hex }"
    	info "table_id: #{ message.table_id }"
    	info "cookie: #{ message.cookie.to_hex }"
    	info "in_port: #{ message.match.in_port }"
        info 'packet_info:'
    	info "  eth_src: #{ message.eth_src }"
    	info "  eth_dst: #{ message.eth_src }"
    	info "  eth_type: #{ message.eth_type.to_hex }"
    	if message.eth_type == 0x800 || message.eth_type == 0x86dd
      	  info "  ip_dscp: #{ message.ip_dscp }"
      	  info "  ip_ecn: #{ message.ip_ecn }"
      	  info "  ip_proto: #{ message.ip_proto }"
    	end
    	if message.vtag?
      	  info "  vlan_vid: #{ message.vlan_vid.to_hex }"
      	  info "  vlan_prio: #{ message.vlan_prio.to_hex }"
      	  info "  vlan_tpid: #{ message.vlan_tpid.to_hex }"
      	  info "  vlan_tci: #{ message.vlan_tci.to_hex }"
    	end
    	if message.ipv4?
      	  info "  ipv4_src: #{ message.ipv4_src }"
      	  info "  ipv4_dst: #{ message.ipv4_dst }"
        end

    	if message.ipv6?
          info "  ipv6_src: #{ message.ipv6_src }"
          info "  ipv6_dst: #{ message.ipv6_dst }"
      	  info "  ipv6_flabel: #{ message.ipv6_flabel.to_hex }"
      	  info "  ipv6_exthdr: #{ message.ipv6_exthdr.to_hex }"
    	end
    	if message.arp?
      	  info "  arp_op: #{ message.arp_op }"
      	  info "  arp_sha: #{ message.arp_sha }"
      	  info "  arp_spa: #{ message.arp_spa }"
      	  info "  arp_tpa: #{ message.arp_tpa }"
    	end

    	if message.icmpv4?
      	  info "  icmpv4_type: #{ message.icmpv4_type.to_hex }"
      	  info "  icmpv4_code: #{ message.icmpv4_code.to_hex }"
    	end

    	if message.icmpv6?
      	  info "  icmpv6_type: #{ message.icmpv6_type.to_hex }"
      	  info "  icmpv6_code: #{ message.icmpv6_code.to_hex }"
    	end
    	if message.udp?
      	  info "  udp_src: #{ message.udp_src.to_hex }"
      	  info "  udp dst: #{ message.udp_dst.to_hex }"
    	end

    	if message.sctp?
      	  info "  sctp_src: #{ message.sctp_src.to_hex }"
      	  info "  sctp_dst: #{ message.sctp_dst.to_hex }"
    	end

    	if message.pbb?
      	  info "  pbb_isid: #{ message.pbb_isid.to_hex }"
    	end

    	if message.mpls?
      	  info "  mpls_label: #{ message.mpls_label.to_hex }"
      	  info "  mpls_tc: #{ message.mpls_tc.to_hex }"
      	  info "  mpls_bos: #{ message.mpls_bos.to_hex }"
    	end
	puts "-----------------------------------------------" 
      end
    end
    ###learn host mac + in_port info
    @fdb.learn macsa, message.in_port
    ###lookup host mac address
    out_port = @fdb.lookup(macda)
    ###if finding the host then annouce it and update the switch flow table
    ###also send packet from packet_in out to out_port
    if out_port
      puts "l2switch found match from what it learnt before!"
      puts "================================================" 
      packet_out datapath_id, message, out_port
      flow_mod datapath_id, macsa, macda, out_port
    else
      ##when mac address is not found in the table
      if(sip.eql? "192.168.0.1") 
          packet_out datapath_id, message, 2 
	  flow_mod datapath_id, macsa,macda, 2
      end
      if(sip.eql? "192.168.0.2")
          packet_out datapath_id, message, 1 
          flow_mod datapath_id, macsa,macda, 1
      end
      
    end
  end

  def flow_removed(_datapath_id, message)
  end

  ##############################################################################

  private

  ##############################################################################
  ### change switch flow table
  def flow_mod(datapath_id, macsa, macda, out_port)
    action = SendOutPort.new(port_number: out_port)
    ins = Instructions::ApplyAction.new(actions: [action])
    send_flow_mod_add(
      datapath_id,
      hard_timeout: 10,
      priority: OFP_DEFAULT_PRIORITY,
      flags: OFPFF_SEND_FLOW_REM,
      match: Match.new(eth_src: macsa, eth_dst: macda),
      instructions: [ins]
    )
  end

  ###send packet from packet_in out to out_port
  def packet_out(datapath_id, message, out_port)
    action = Actions::SendOutPort.new(port_number: out_port)
    send_packet_out(
      datapath_id,
      packet_in: message,
      actions: [action]
    )
  end
end

### Local variables:
### mode: Ruby
### coding: utf-8
### indent-tabs-mode: nil
### End:
