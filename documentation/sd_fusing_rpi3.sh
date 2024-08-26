#!/bin/bash

declare FORMAT=""
declare DEVICE=""
declare SKIP=0
declare -i OLD_DD=0

# Binaires array for fusing
declare -a FUSING_BINARY_ARRAY
declare -i FUSING_BINARY_NUM=0

declare CONV_ASCII=""
declare -i FUS_ENTRY_NUM=0
declare -i ab_option=0

# binary name | part number | bs | label | fs type
declare -a PART_TABLE=(
	"boot.img"			1	4M	boot_a			vfat
	"rootfs.img"			2	4M	rootfs_a		ext4
	"system-data.img"		3	4M	system-data		ext4
	"user.img"			5	4M	user			ext4
	"modules.img"			6	4M	module_a		ext4
	"ramdisk.img"			7	4M	ramdisk_a		ext4
	"ramdisk-recovery.img"		8	4M	ramdisk-recovery_a	ext4
	"hal.img"			10	4M	hal_a			ext4
	"boot.img"			11	4M	boot_b			vfat
	"rootfs.img"			12	4M	rootfs_b		ext4
	"modules.img"			13	4M	module_b		ext4
	"ramdisk.img"			14	4M	ramdisk_b		ext4
	"ramdisk-recovery.img"		15	4M	ramdisk-recovery_b	ext4
	"hal.img"			16	4M	hal_b			ext4
	)
declare -a PART_TABLE_B=(
	"boot.img"			11	4M	boot_b			vfat
	"rootfs.img"			12	4M	rootfs_b		ext4
	"modules.img"			13	4M	module_b		ext4
	"ramdisk.img"			14	4M	ramdisk_b		ext4
	"ramdisk-recovery.img"		15	4M	ramdisk-recovery_b	ext4
	"hal.img"			16	4M	hal_b			ext4
	)

declare -r -i PART_TABLE_COL=5
declare -r -i PART_TABLE_ROW=${#PART_TABLE[*]}/${PART_TABLE_COL}
declare -r -i PART_TABLE_ROW_B=${#PART_TABLE_B[*]}/${PART_TABLE_COL}

# partition table support
function get_index_use_name () {
	local -r binary_name=$1

	for ((idx=0;idx<$PART_TABLE_ROW;idx++)); do
		if [ ${PART_TABLE[idx * ${PART_TABLE_COL} + 0]} == "$binary_name" ]; then
			return $idx
		fi
	done

	# return out of bound index
	return $idx
}

# partition table support
function get_index_use_name_to_b () {
	local -r binary_name=$1

	for ((idx=0;idx<$PART_TABLE_ROW_B;idx++)); do
		if [ ${PART_TABLE_B[idx * ${PART_TABLE_COL} + 0]} == "$binary_name" ]; then
			return $idx
		fi
	done

	# return out of bound index
	return $idx
}

function print_message () {
	local color=$1
	local message=$2

	tput setaf $color
	tput bold
	echo ""
	echo $message
	tput sgr 0
}

function check_ddversion () {
	# NOTE
	# before coreutils dd 8.24, dd doesn't support "status=progress"
	# and the option causes fusing failure. For backward compatibility,
	# do not use the option for old dd
	local version=(`dd --version | head -1 | grep -o '[0-9]\+'`)
	local major=${version[0]}
	local minor=${version[1]}

	if [ $major -lt 8 ];  then
		OLD_DD=1
	elif [ $major -eq 8 -a $minor -lt 24 ];  then
		OLD_DD=1
	fi
}

function fusing_image () {
	if [ $ab_option == 2 ] ; then
		echo "Skip to update Partitoin A"
		return
	fi
	local -r fusing_img=$1

	# get binary info using basename
	get_index_use_name $(basename "$fusing_img")
	local -r -i part_idx=$?

	if [ $part_idx -ne $PART_TABLE_ROW ];then
		local -r num=${PART_TABLE[${part_idx} * ${PART_TABLE_COL} + 1]}
		if [ "${num}" == "" ]; then
			local -r blktype=disk
		else
			local -r blktype=part
		fi
		local -r device=/dev/`lsblk ${DEVICE} -o TYPE,KNAME | awk "/^${blktype}.*[a-z]${num}\$/ { print \\\$2 }"`
		local -r bs=${PART_TABLE[${part_idx} * ${PART_TABLE_COL} + 2]}
	else
		echo "Unsupported binary: $fusing_img"
		return
	fi

	local -r input_size=`du -b $fusing_img | awk '{print $1}'`
	local -r input_size_mb=`expr $input_size / 1024 / 1024`

	print_message 2 "[Fusing $1 ($input_size_mb MiB)]"
	if [ "$blktype" == "part" ]; then
		local MOUNT_PART=`mount | grep $device`
		if [ "$MOUNT_PART" != "" ]; then
			umount $device
		fi
	fi
	if [ $OLD_DD == 1 ]; then
		dd if=$fusing_img | pv -s $input_size | dd of=$device bs=$bs
	else
		dd if=$fusing_img of=$device bs=$bs status=progress oflag=direct
	fi
}

function fusing_image_to_b () {
	if [ $ab_option == 0 ]  ; then
		echo "Skip to update Partitoin B"
		return
	fi
	local -r fusing_img=$1

	# get binary info using basename
	get_index_use_name_to_b $(basename "$fusing_img")

	local -r -i part_idx=$?

	if [ $part_idx -ne $PART_TABLE_ROW_B ];then
		local -r num=${PART_TABLE_B[${part_idx} * ${PART_TABLE_COL} + 1]}
		if [ "${num}" == "" ]; then
			local -r blktype=disk
		else
			local -r blktype=part
		fi
		local -r device=/dev/`lsblk ${DEVICE} -o TYPE,KNAME | awk "/^${blktype}.*[a-z]${num}\$/ { print \\\$2 }"`
		local -r bs=${PART_TABLE_B[${part_idx} * ${PART_TABLE_COL} + 2]}
	else
		echo "Unsupported binary: $fusing_img"
		return
	fi

	local -r input_size=`du -b $fusing_img | awk '{print $1}'`
	local -r input_size_mb=`expr $input_size / 1024 / 1024`

	print_message 2 "[Fusing $1 ($input_size_mb MiB)]"
	if [ "$blktype" == "part" ]; then
		local MOUNT_PART=`mount | grep $device`
		if [ "$MOUNT_PART" != "" ]; then
			umount $device
		fi
	fi
	if [ $OLD_DD == 1 ]; then
		dd if=$fusing_img | pv -s $input_size | dd of=$device bs=$bs
	else
		dd if=$fusing_img of=$device bs=$bs status=progress oflag=direct
	fi
}

function fuse_image_tarball () {
	local -r filepath=$1
	local -r temp_dir="tar_tmp"

	mkdir -p $temp_dir
	tar xvf $filepath -C $temp_dir
	cd $temp_dir

	for file in *
	do
		fusing_image $file
		fusing_image_to_b $file
	done

	cd ..
	rm -rf $temp_dir
	eval sync
}

function initialize_parameter () {
	# create "reboot-param.bin" file in inform partition for passing reboot parameter
	# It should be done only once upon partition format.
	local -r DISK=$DEVICE
	local -r PART9=/dev/`lsblk ${DISK} -o TYPE,KNAME | grep part | awk '{ print $2 }' | grep -G "[a-z]9\$"`

	if [ -d mnt_tmp ]; then
		echo "Remove the existing mnt_tmp directory!!"
		rm -rf mnt_tmp
	fi
	mkdir mnt_tmp
	mount -t ext4 ${PART9} ./mnt_tmp
	echo "norm" > ./mnt_tmp/reboot-param.bin
	echo "norm" > ./mnt_tmp/reboot-param.info
	echo "a" > ./mnt_tmp/partition-ab.info
	echo "1" > ./mnt_tmp/partition-ab-cloned.info
	echo "0" > ./mnt_tmp/upgrade-status.info

	# To check the status of partition. (default "ok")
	echo "ok" > ./mnt_tmp/partition-a-status.info
	echo "ok" > ./mnt_tmp/partition-b-status.info

	sync
	umount ./mnt_tmp
	rmdir mnt_tmp
}

function fuse_image () {

	if [ "$FUSING_BINARY_NUM" == 0 ]; then
		return
	fi

	# Clear preivous values before flashing image
	initialize_parameter

	for ((fuse_idx = 0 ; fuse_idx < $FUSING_BINARY_NUM ; fuse_idx++))
	do
		local filename=${FUSING_BINARY_ARRAY[fuse_idx]}

		case "$filename" in
		    *.tar | *.tar.gz)
			fuse_image_tarball $filename
			;;
		    *)
			fusing_image $filename
			fusing_image_to_b $filename
			;;
		esac
	done
	echo ""
}

# partition format
function mkpart_3 () {
	# NOTE: if your sfdisk version is less than 2.26.0, then you should use following sfdisk command:
	# sfdisk --in-order --Linux --unit M $DISK <<-__EOF__

	# NOTE: sfdisk 2.26 doesn't support units other than sectors and marks --unit option as deprecated.
	# The input data needs to contain multipliers (MiB) instead.
	local version=(`sfdisk -v | grep -o '[0-9]\+'`)
	local major=${version[0]}
	local minor=${version[1]}
	local sfdisk_new=0
	local support_delete=0

	if [ $major -gt 2 ];  then
		sfdisk_new=1
		if [ $major -eq 2 -a $minor -ge 28 ]; then
			support_delete=1
		fi
	else
		if [ $major -eq 2 -a $minor -ge 26 ];  then
			sfdisk_new=1
		fi
	fi

	if [ $sfdisk_new == 0 ]; then
		echo "$(tput setaf 3)$(tput bold)NOTICE: Your sfdisk ${version[0]}.${version[1]}  version is too old. Update Latest sfdisk!"
		tput sgr 0
		exit -1
	fi

	local -r DISK=$DEVICE
	local -r SIZE=`sfdisk -s $DISK`
	local -r SIZE_MB=$((SIZE >> 10))

	local -r BOOT_SZ=64
	local -r ROOTFS_SZ=3072
	local -r DATA_SZ=1344
	local -r MODULE_SZ=32
	local -r RAMDISK_SZ=32
	local -r RAMDISK_RECOVERY_SZ=32
	local -r INFORM_SZ=8
	local -r HAL_SZ=256
	local -r PARAM_SZ=4
	local -r RESERVED1_SZ=64
	local -r RESERVED2_SZ=125
	local -r EXTEND_SZ=36

	let "USER_SZ = $SIZE_MB - $BOOT_SZ * 2 - $ROOTFS_SZ * 2 - $DATA_SZ - $MODULE_SZ * 2 - $RAMDISK_SZ * 2 - $RAMDISK_RECOVERY_SZ * 2 - $INFORM_SZ - $EXTEND_SZ - $HAL_SZ * 2 - $RESERVED1_SZ - $RESERVED2_SZ - $PARAM_SZ"

	local -r BOOT_A=${PART_TABLE[0 * ${PART_TABLE_COL} + 3]}
	local -r ROOTFS_A=${PART_TABLE[1 * ${PART_TABLE_COL} + 3]}
	local -r SYSTEMDATA=${PART_TABLE[2 * ${PART_TABLE_COL} + 3]}
	local -r USER=${PART_TABLE[3 * ${PART_TABLE_COL} + 3]}
	local -r MODULE_A=${PART_TABLE[4 * ${PART_TABLE_COL} + 3]}
	local -r RAMDISK_A=${PART_TABLE[5 * ${PART_TABLE_COL} + 3]}
	local -r RAMDISK_RECOVERY_A=${PART_TABLE[6 * ${PART_TABLE_COL} + 3]}
	local -r INFORM=inform
	local -r HAL_A=${PART_TABLE[7 * ${PART_TABLE_COL} + 3]}
	local -r BOOT_B=${PART_TABLE[8 * ${PART_TABLE_COL} + 3]}
	local -r ROOTFS_B=${PART_TABLE[9 * ${PART_TABLE_COL} + 3]}
	local -r MODULE_B=${PART_TABLE[10 * ${PART_TABLE_COL} + 3]}
	local -r RAMDISK_B=${PART_TABLE[11 * ${PART_TABLE_COL} + 3]}
	local -r RAMDISK_RECOVERY_B=${PART_TABLE[12 * ${PART_TABLE_COL} + 3]}
	local -r HAL_B=${PART_TABLE[13 * ${PART_TABLE_COL} + 3]}
	local -r RESERVED0=reserved0
	local -r RESERVED1=reserved1
	local -r RESERVED2=reserved2

	if [[ $USER_SZ -le 100 ]]
	then
		echo "We recommend to use more than 8GB disk"
		exit 0
	fi

	echo "================================================"
	echo "Label			dev		size"
	echo "================================================"
	echo $BOOT_A"			" $DISK"1	" $BOOT_SZ "MB"
	echo $ROOTFS_A"			" $DISK"2	" $ROOTFS_SZ "MB"
	echo $SYSTEMDATA"		" $DISK"3	" $DATA_SZ "MB"
	echo "[Extend]""		" $DISK"4"
	echo " "$USER"			" $DISK"5	" $USER_SZ "MB"
	echo " "$MODULE_A"		" $DISK"6	" $MODULE_SZ "MB"
	echo " "$RAMDISK_A"		" $DISK"7	" $RAMDISK_SZ "MB"
	echo " "$RAMDISK_RECOVERY_A"	" $DISK"8	" $RAMDISK_RECOVERY_SZ "MB"
	echo " "$INFORM"		" $DISK"9	" $INFORM_SZ "MB"
	echo " "$HAL_A"			" $DISK"10	" $HAL_SZ "MB"
	echo " "$BOOT_B"		" $DISK"11	" $BOOT_SZ "MB"
	echo " "$ROOTFS_B"		" $DISK"12	" $ROOTFS_SZ "MB"
	echo " "$MODULE_B"		" $DISK"13	" $MODULE_SZ "MB"
	echo " "$RAMDISK_B"		" $DISK"14	" $RAMDISK_SZ "MB"
	echo " "$RAMDISK_RECOVERY_B"	" $DISK"15	" $RAMDISK_RECOVERY_SZ "MB"
	echo " "$HAL_B"			" $DISK"16	" $HAL_SZ "MB"
	echo " "$RESERVED0"		" $DISK"17	" $PARAM_SZ "MB"
	echo " "$RESERVED1"		" $DISK"18	" $RESERVED1_SZ "MB"
	echo " "$RESERVED2"		" $DISK"19	" $RESERVED2_SZ "MB"

	local MOUNT_LIST=`mount | grep $DISK | awk '{print $1}'`
	for mnt in $MOUNT_LIST
	do
		umount $mnt
	done

	echo "Remove partition table..."
	dd if=/dev/zero of=$DISK bs=512 count=32 conv=notrunc

	if [ $support_delete == 1 ]; then
		sfdisk --delete $DISK
	fi

	SCRIPT=""
	for ((idx=0; idx < $PART_TABLE_ROW; idx++)); do
		NR=${PART_TABLE[idx * ${PART_TABLE_COL} + 1]}
		eval "PART_LABEL_NR_${NR}=${PART_TABLE[idx * ${PART_TABLE_COL} + 3]}"
	done

	sfdisk $DISK <<-__EOF__
	label: gpt
	start=4MiB, size=${BOOT_SZ}MiB, type= C12A7328-F81F-11D2-BA4B-00A0C93EC93B, name=${PART_LABEL_NR_1}
	size=${ROOTFS_SZ}MiB, name=${PART_LABEL_NR_2}
	size=${DATA_SZ}MiB, name=${PART_LABEL_NR_3}
	size=${EXTEND_SZ}MiB, name=none
	size=${USER_SZ}MiB, name=${PART_LABEL_NR_5}
	size=${MODULE_SZ}MiB, name=${PART_LABEL_NR_6}
	size=${RAMDISK_SZ}MiB, name=${PART_LABEL_NR_7}
	size=${RAMDISK_RECOVERY_SZ}MiB, name=${PART_LABEL_NR_8}
	size=${INFORM_SZ}MiB, name=inform
	size=${HAL_SZ}MiB, name=${PART_LABEL_NR_10}
	size=${BOOT_SZ}MiB, type= C12A7328-F81F-11D2-BA4B-00A0C93EC93B, name=${PART_LABEL_NR_11}
	size=${ROOTFS_SZ}MiB, name=${PART_LABEL_NR_12}
	size=${MODULE_SZ}MiB, name=${PART_LABEL_NR_13}
	size=${RAMDISK_SZ}MiB, name=${PART_LABEL_NR_14}
	size=${RAMDISK_RECOVERY_SZ}MiB, name=${PART_LABEL_NR_15}
	size=${HAL_SZ}MiB, name=${PART_LABEL_NR_16}
	size=${PARAM_SZ}MiB, name=reserved0
	size=${RESERVED1_SZ}MiB, name=reserved1
	size=${RESERVED2_SZ}MiB, name=reserved2
	__EOF__


	for ((idx=0;idx<$PART_TABLE_ROW;idx++)); do
		local PART=/dev/`lsblk ${DISK} -o TYPE,KNAME | awk "/^part.*[a-z]${PART_TABLE[$idx * ${PART_TABLE_COL} + 1]}\$/ { print \\\$2 }"`
		if [ "${PART_TABLE[$idx * ${PART_TABLE_COL} + 4]}" == "vfat" ]; then
			mkfs.vfat -F 16 ${PART} -n ${PART_TABLE[$idx * ${PART_TABLE_COL} + 3]}
			if [ $? -eq 1 ]; then
				echo "Failed to format as FAT filesystem"
				exit -1
			fi
		elif [ "${PART_TABLE[$idx * ${PART_TABLE_COL} + 4]}" == "ext4" ]; then
			mkfs.ext4 -q ${PART} -L ${PART_TABLE[$idx * ${PART_TABLE_COL} + 3]} -F
		else
			echo "Skip to format for unknown filesystem type ${PART_TABLE[$idx * ${PART_TABLE_COL} + 4]} for part$idx, ${PART_TABLE[$idx * ${PART_TABLE_COL} + 3]}"
		fi
	done

	local -r PART9=/dev/`lsblk ${DISK} -o TYPE,KNAME | grep part | awk '{ print $2 }' | grep -G "[a-z]9\$"`
	mkfs.ext4 -q ${PART9} -L $INFORM -F -O ^metadata_csum

	# initialize value of parameters
	initialize_parameter

	local -r PART17=/dev/`lsblk ${DISK} -o TYPE,KNAME | grep part | awk '{ print $2 }' | grep -G "[a-z]17\$"`
	mkfs.ext4 -q ${PART17} -L $RESERVED0 -F

	local -r PART18=/dev/`lsblk ${DISK} -o TYPE,KNAME | grep part | awk '{ print $2 }' | grep -G "[a-z]18\$"`
	mkfs.ext4 -q ${PART18} -L $RESERVED1 -F

	local -r PART19=/dev/`lsblk ${DISK} -o TYPE,KNAME | grep part | awk '{ print $2 }' | grep -G "[a-z]19\$"`
	mkfs.ext4 -q ${PART19} -L $RESERVED2 -F
}

function skip_resize () {
	if [ "${SKIP}" == "0" ]; then
		return 0;
	fi

	if [ ! -d mnt_tmp ] ; then
		mkdir mnt_tmp
	fi

	mount -t ext4 ${DEVICE}3 ./mnt_tmp
	touch ./mnt_tmp/var/.resizefs_done

	echo "Rootfs resize will be skipped..."
	sync
	umount ./mnt_tmp
	rmdir mnt_tmp
}

function show_usage () {
	echo "- Usage:"
	echo "	sudo ./sd_fusing*.sh -d <device> [-b <path> <path> ..] [--format] [--update [b] ]"
	echo "  -d  : device ndoe "
	echo "  -b  : binary "
	echo "  --update : If want to update Image about B Partition, use --update option with b"
	echo "		   Otherwise, it will be updated to both partition"
}

function check_partition_format () {
	if [ "$FORMAT" != "2" ]; then
		echo "-----------------------"
		echo "Skip $DEVICE format"
		echo "-----------------------"
		return 0
	fi

	echo "-------------------------------"
	echo "Start $DEVICE format"
	echo ""
	mkpart_3
	echo "End $DEVICE format"
	echo "-------------------------------"
	echo ""
}

function check_args () {
	if [ "$DEVICE" == "" ]; then
		echo "$(tput setaf 1)$(tput bold)- Device node is empty!"
		show_usage
		tput sgr 0
		exit 0
	fi

	if [ "$DEVICE" != "" ]; then
		echo "Device: $DEVICE"
	fi

	if [ "$FUSING_BINARY_NUM" != 0 ]; then
		echo "Fusing binary: "
		for ((bid = 0 ; bid < $FUSING_BINARY_NUM ; bid++))
		do
			echo "  ${FUSING_BINARY_ARRAY[bid]}"
		done
		echo ""
	fi

	if [ "$FORMAT" == "1" ]; then
		echo ""
		echo -n "$(tput setaf 3)$(tput bold)$DEVICE will be formatted, Is it OK? [y/<n>] "
		tput sgr 0
		read input
		if [ "$input" == "y" ] || [ "$input" == "Y" ]; then
			FORMAT=2
		else
			FORMAT=0
		fi
	fi
}

function check_device () {
	if [ ! -b "$DEVICE" ]; then
		echo "No such device: $DEVICE"
		exit 0
	fi

	DEVICE=/dev/`lsblk $DEVICE -o TYPE,KNAME | awk '/^(disk|loop)/ { print $2 }'`

	local REMOVABLE=`lsblk $DEVICE -nd -o RM | grep 1 | wc -l`
	local LOOPBACK=`lsblk $DEVICE -nd -o TYPE | grep loop | wc -l`
	if [ "$REMOVABLE" == "0" -a "$LOOPBACK" = "0" ]; then
		echo ""
		echo -n "$(tput setaf 3)$(tput bold)$DEVICE is neither a removable disk nor a loopback, Is it OK? [y/<n>] "
		tput sgr 0
		read input
		if [ "$input" != "y" ] && [ "$input" != "Y" ]; then
			exit 0
		fi
	fi

	if [ ! -w "$DEVICE" ]; then
		echo "Write not permitted: $DEVICE"
		exit 0
	fi
}

function print_logo () {
	echo ""
	echo "Raspberry Pi4 downloader, version 1.0.13"
	echo "$(tput setaf 1)$(tput bold)NOTE: To use this script, it has to update to latest eeprom"
	echo ""
}

print_logo

function add_fusing_binary() {
	local declare binary_name=$1

	if [ "$binary_name" != "" ]; then
		if [ -f "$binary_name" ]; then
			FUSING_BINARY_ARRAY[$FUSING_BINARY_NUM]=$binary_name

			FUSING_BINARY_NUM=$((FUSING_BINARY_NUM + 1))
		else
			echo "No such file: $binary_name"
		fi
	fi
}


declare -i binary_option=0

while test $# -ne 0; do
	option=$1
	shift

	case $option in
	--f | --format)
		FORMAT="1"
		binary_option=0
		;;
	-d)
		DEVICE=$1
		binary_option=0
		shift
		;;
	-b)
		add_fusing_binary $1
		binary_option=1
		shift
		;;
	--update)
		if [ "$1" == "b" ] ; then
			ab_option=2
		else
			ab_option=1
		fi
		shift
		;;
	--skip-resize)
		SKIP=1
		shift
		;;
	*)
		if [ $binary_option == 1 ];then
			add_fusing_binary $option
		else
			echo "Unkown command: $option"
			exit
		fi
		;;
	esac
done

check_args
check_device
check_partition_format
check_ddversion
fuse_image
skip_resize
