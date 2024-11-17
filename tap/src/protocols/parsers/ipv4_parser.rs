use std::sync::Arc;

use anyhow::{bail, Result};
use byteorder::{BigEndian, ByteOrder};
use chrono::{DateTime, Utc};
use crate::helpers::network::to_ipv4_address;
use crate::wired::packets::{EthernetPacket, IPv4Packet};

pub fn parse_from_ethernet(packet: &Arc<EthernetPacket>) -> Result<IPv4Packet> {
    if packet.data.is_empty() {
        bail!("Empty payload.")
    }

    parse(
        &packet.data,
        Some(packet.source_mac.clone()),
        Some(packet.destination_mac.clone()),
        packet.timestamp
    )
}

// Parses a Raw IPv4 Packet that does not include any prepending headers like Ethernet.
pub fn parse_raw(packet: &[u8], timestamp: DateTime<Utc>) -> Result<IPv4Packet> {
    parse(packet, None, None, timestamp)
}

fn parse(packet: &[u8], 
         source_mac: Option<String>,
         destination_mac: Option<String>,
         timestamp: DateTime<Utc>) -> Result<IPv4Packet> {
    let header_length = match ((&packet[0] & 0x0F) as usize)
        .checked_mul(32)
        .and_then(|result| result.checked_div(8)) {
        Some(hl) => hl,
        None => { bail!("Header length calculation failed with too large numbers.") }
    };

    if packet.len() < header_length || packet.len() < 20  {
        bail!("Payload too short.");
    }

    let total_length = BigEndian::read_u16(&packet[2..4]) as usize;

    if total_length < header_length || total_length > packet.len() {
        bail!("Invalid total length.");
    }

    let ttl = packet[8];
    let protocol = packet[9];
    let source_address = to_ipv4_address(&packet[12..16]);
    let destination_address = to_ipv4_address(&packet[16..20]);
    let payload = packet[header_length..].to_vec();
    let size = packet.len();

    Ok(IPv4Packet {
        source_mac,
        destination_mac,
        header_length,
        total_length,
        source_address,
        destination_address,
        ttl,
        protocol,
        payload,
        size: size as u32,
        timestamp
    })
}