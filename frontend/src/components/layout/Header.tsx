import { Link } from "@tanstack/react-router";
import { LOGIN_URL, logout } from "@/api/client";
import { useCurrentMember } from "@/features/member/queries";

export function Header() {
  const { data: member, isPending } = useCurrentMember();

  return (
    <header className="border-b border-slate-200">
      <div className="mx-auto flex max-w-4xl items-center justify-between px-4 py-3">
        <Link to="/" className="text-lg font-semibold">
          게시판
        </Link>
        <nav className="flex items-center gap-3 text-sm">
          {isPending ? null : member ? (
            <>
              <span className="text-slate-700">{member.nickname}</span>
              <button
                type="button"
                onClick={() => void logout()}
                className="text-slate-500 hover:text-slate-900"
              >
                로그아웃
              </button>
            </>
          ) : (
            <a href={LOGIN_URL} className="text-slate-500 hover:text-slate-900">
              로그인
            </a>
          )}
        </nav>
      </div>
    </header>
  );
}
