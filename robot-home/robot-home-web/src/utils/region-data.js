/**
 * 中国省份-城市级联数据（精简版，用于询价表单地区选择）
 * 数据格式: [{ value, label, children: [{ value, label }] }]
 */
export const regionOptions = [
  { value: '北京', label: '北京', children: [{ value: '北京', label: '北京' }] },
  { value: '天津', label: '天津', children: [{ value: '天津', label: '天津' }] },
  { value: '上海', label: '上海', children: [{ value: '上海', label: '上海' }] },
  { value: '重庆', label: '重庆', children: [{ value: '重庆', label: '重庆' }] },
  { value: '河北', label: '河北', children: [
    { value: '石家庄', label: '石家庄' }, { value: '唐山', label: '唐山' }, { value: '保定', label: '保定' },
    { value: '邯郸', label: '邯郸' }, { value: '廊坊', label: '廊坊' }, { value: '秦皇岛', label: '秦皇岛' }
  ]},
  { value: '山西', label: '山西', children: [
    { value: '太原', label: '太原' }, { value: '大同', label: '大同' }, { value: '临汾', label: '临汾' }
  ]},
  { value: '辽宁', label: '辽宁', children: [
    { value: '沈阳', label: '沈阳' }, { value: '大连', label: '大连' }, { value: '鞍山', label: '鞍山' }
  ]},
  { value: '吉林', label: '吉林', children: [
    { value: '长春', label: '长春' }, { value: '吉林', label: '吉林' }
  ]},
  { value: '黑龙江', label: '黑龙江', children: [
    { value: '哈尔滨', label: '哈尔滨' }, { value: '大庆', label: '大庆' }
  ]},
  { value: '江苏', label: '江苏', children: [
    { value: '南京', label: '南京' }, { value: '苏州', label: '苏州' }, { value: '无锡', label: '无锡' },
    { value: '常州', label: '常州' }, { value: '南通', label: '南通' }, { value: '徐州', label: '徐州' }
  ]},
  { value: '浙江', label: '浙江', children: [
    { value: '杭州', label: '杭州' }, { value: '宁波', label: '宁波' }, { value: '温州', label: '温州' },
    { value: '嘉兴', label: '嘉兴' }, { value: '绍兴', label: '绍兴' }
  ]},
  { value: '安徽', label: '安徽', children: [
    { value: '合肥', label: '合肥' }, { value: '芜湖', label: '芜湖' }, { value: '蚌埠', label: '蚌埠' }
  ]},
  { value: '福建', label: '福建', children: [
    { value: '福州', label: '福州' }, { value: '厦门', label: '厦门' }, { value: '泉州', label: '泉州' }
  ]},
  { value: '江西', label: '江西', children: [
    { value: '南昌', label: '南昌' }, { value: '赣州', label: '赣州' }
  ]},
  { value: '山东', label: '山东', children: [
    { value: '济南', label: '济南' }, { value: '青岛', label: '青岛' }, { value: '烟台', label: '烟台' },
    { value: '潍坊', label: '潍坊' }, { value: '临沂', label: '临沂' }
  ]},
  { value: '河南', label: '河南', children: [
    { value: '郑州', label: '郑州' }, { value: '洛阳', label: '洛阳' }, { value: '开封', label: '开封' }
  ]},
  { value: '湖北', label: '湖北', children: [
    { value: '武汉', label: '武汉' }, { value: '宜昌', label: '宜昌' }, { value: '襄阳', label: '襄阳' }
  ]},
  { value: '湖南', label: '湖南', children: [
    { value: '长沙', label: '长沙' }, { value: '株洲', label: '株洲' }, { value: '湘潭', label: '湘潭' }
  ]},
  { value: '广东', label: '广东', children: [
    { value: '广州', label: '广州' }, { value: '深圳', label: '深圳' }, { value: '东莞', label: '东莞' },
    { value: '佛山', label: '佛山' }, { value: '珠海', label: '珠海' }, { value: '惠州', label: '惠州' },
    { value: '中山', label: '中山' }
  ]},
  { value: '广西', label: '广西', children: [
    { value: '南宁', label: '南宁' }, { value: '柳州', label: '柳州' }, { value: '桂林', label: '桂林' }
  ]},
  { value: '海南', label: '海南', children: [
    { value: '海口', label: '海口' }, { value: '三亚', label: '三亚' }
  ]},
  { value: '四川', label: '四川', children: [
    { value: '成都', label: '成都' }, { value: '绵阳', label: '绵阳' }, { value: '德阳', label: '德阳' }
  ]},
  { value: '贵州', label: '贵州', children: [
    { value: '贵阳', label: '贵阳' }, { value: '遵义', label: '遵义' }
  ]},
  { value: '云南', label: '云南', children: [
    { value: '昆明', label: '昆明' }, { value: '大理', label: '大理' }
  ]},
  { value: '西藏', label: '西藏', children: [
    { value: '拉萨', label: '拉萨' }
  ]},
  { value: '陕西', label: '陕西', children: [
    { value: '西安', label: '西安' }, { value: '咸阳', label: '咸阳' }
  ]},
  { value: '甘肃', label: '甘肃', children: [
    { value: '兰州', label: '兰州' }
  ]},
  { value: '青海', label: '青海', children: [
    { value: '西宁', label: '西宁' }
  ]},
  { value: '宁夏', label: '宁夏', children: [
    { value: '银川', label: '银川' }
  ]},
  { value: '新疆', label: '新疆', children: [
    { value: '乌鲁木齐', label: '乌鲁木齐' }
  ]},
  { value: '内蒙古', label: '内蒙古', children: [
    { value: '呼和浩特', label: '呼和浩特' }, { value: '包头', label: '包头' }, { value: '鄂尔多斯', label: '鄂尔多斯' }
  ]},
  { value: '香港', label: '香港', children: [{ value: '香港', label: '香港' }] },
  { value: '澳门', label: '澳门', children: [{ value: '澳门', label: '澳门' }] },
  { value: '台湾', label: '台湾', children: [{ value: '台北', label: '台北' }] }
]