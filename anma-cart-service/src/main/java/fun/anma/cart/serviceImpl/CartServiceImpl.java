package fun.anma.cart.serviceImpl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;

import fun.anma.cart.service.CartService;
import fun.anma.common.pojo.AnmaResult;
import fun.anma.common.pojo.CartItem;
import fun.anma.jedis.service.JedisClient;
import fun.anma.mapper.TbItemMapper;
import fun.anma.pojo.TbItem;
import fun.anma.pojo.TbUser;

@Service
public class CartServiceImpl implements CartService{

	@Autowired
	private TbItemMapper itemMapper;
	@Autowired
	private JedisClient jedisClient;
	@Value("${USER_SESSION}")
	private String USER_SESSION;
	
	@Value("${CART_PRE}")
	private String CART_PRE;
	
	@Override
	public AnmaResult addItemCart(String token, Long itemId, Integer num) {
		Long userId = getUserIdByToken(token);
		//判断添加的商品是否已经存在，如果存在就数量叠加，不存在就重新添加
		String itemInfo = jedisClient.hget(CART_PRE+":"+userId, itemId+"");
		//如果存在，给数量加num
		if (itemInfo!=null) {
			CartItem cItem = JSON.parseObject(itemInfo,CartItem.class);
			//数量相加
			cItem.setNum(cItem.getNum()+num);
			//写会redis
			jedisClient.hset(CART_PRE+":"+userId, itemId+"", JSON.toJSONString(cItem));
			return AnmaResult.ok();
		}
		//如果不存在就新创建
		TbItem item = itemMapper.selectByPrimaryKey(itemId);
		//为购物车中的商品添加属性
		CartItem cartItem = new CartItem();
		cartItem.setId(itemId);
		cartItem.setNum(num);
		cartItem.setPrice(item.getPrice());
		//取第一张图片作为购物车显示图片
		String image = item.getImage();
		if (StringUtils.isNotBlank(image)) {
			image = image.split(",")[0];
		}
		cartItem.setImage(image);
		cartItem.setTitle(item.getTitle());
		
		String jsonString = JSON.toJSONString(cartItem);
		//写到redis中
		jedisClient.hset(CART_PRE+":"+userId, itemId+"", jsonString);
		
		return AnmaResult.ok();
	}
	
	
	
	//根据token获取用户id
	public Long getUserIdByToken(String token){
		//根据token获取用户信息
		String string = jedisClient.get(USER_SESSION+":"+token);
		//转化为user
		TbUser user = JSON.parseObject(string, TbUser.class);
		Long id = user.getId();
		return id;
	}

	@Override
	public AnmaResult updateItemNum(String token, Long itemId, Integer num) {
		Long userId = getUserIdByToken(token);
		//从redis中取商品信息
		String itemInfo = jedisClient.hget(CART_PRE+":"+userId, itemId+"");
		CartItem cartItem = JSON.parseObject(itemInfo, CartItem.class);
		//修改商品信息
		cartItem.setNum(num);
		//保存到redis
		jedisClient.hset(CART_PRE+":"+userId, itemId+"", JSON.toJSONString(cartItem));
		return AnmaResult.ok();
	}



	@Override
	public AnmaResult deleteItem(String token, Long itemId) {
		Long userId = getUserIdByToken(token);
		//从redis中删掉商品信息
		//System.out.println("进入delservice");
		//System.out.println(itemId);
		Long hdel = jedisClient.hdel(CART_PRE+":"+userId, itemId+"");
		//System.out.println(hdel);
		return AnmaResult.ok();
	}



	@Override
	public List<CartItem> showCartList(String token) {
		Long userId = getUserIdByToken(token);
		//获取所有信息
		List<String> hvals = jedisClient.hvals(CART_PRE+":"+userId);
		//将json字符串转化为对象,转存到List数组中
		ArrayList<CartItem> list = new ArrayList<CartItem>();
		
		for (String string : hvals) {
			CartItem cartItem = JSON.parseObject(string, CartItem.class);
			list.add(cartItem);
		}
		return list;
	}
	

}
