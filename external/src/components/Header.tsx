type HeaderProps = {
  innerText: string;
};

const Header = (props: HeaderProps) => {
  return (
    <div className="m-5">
      <h1 className="text-center text-blue-500 text-3xl">{props.innerText}</h1>
    </div>
  );
};

export default Header;
